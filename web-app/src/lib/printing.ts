import {
  displaySenderPhone,
  ORDER_STATUS_META,
  PAYMENT_METHOD_META,
  PAYMENT_STATUS_LABELS,
  orderStatusLabelFor,
  type Order,
} from '../types/models';
import { formatCurrency, formatDateTime, formatDigits, formatNumber, normalizeWhatsAppPhone } from './formatters';

export type ThermalPaperWidth = '58mm' | '80mm';

export function buildWhatsAppInvoiceText(
  order: Order,
  storeName: string,
  storePhone: string,
): string {
  const cleanStore = storeName.trim() || 'الإيمان للتجارة';
  const isPickup = order.fulfillmentType === 'pickup';
  const recipient = order.recipientName || order.customerName;
  const address = [order.governorate, order.city, order.streetAddress].filter(Boolean).join(' - ');

  const lines: string[] = [
    `🧾 *فاتورة طلب من ${cleanStore}*`,
    `رقم الطلب: *#${formatDigits(order.orderNumber)}*`,
    `طريقة الاستلام: ${isPickup ? 'الاستلام من المتجر' : 'توصيل إلى العنوان'}`,
    `العميل: ${recipient}`,
  ];
  if (order.phone) lines.push(`الهاتف: ${formatDigits(order.phone)}`);
  if (isPickup) {
    lines.push(`مقر الاستلام: ${order.streetAddress || cleanStore}`);
  } else if (address) {
    lines.push(`العنوان: ${address}`);
  }
  lines.push(`الحالة: ${orderStatusLabelFor(order.orderStatus, isPickup)}`);
  const sender = displaySenderPhone(order);
  if (sender) lines.push(`الرقم المحوَّل منه: ${formatDigits(sender)}`);
  lines.push('------------------');
  order.items.forEach((item, idx) => {
    const variantParts = [
      item.selectedColor ? `اللون: ${item.selectedColor}` : '',
      item.selectedSize ? `المقاس: ${item.selectedSize}` : '',
    ].filter(Boolean);
    const variantSuffix = variantParts.length ? ` [${variantParts.join(' • ')}]` : '';
    lines.push(
      `${formatNumber(idx + 1)}. ${item.productName}${variantSuffix} (${formatNumber(item.quantity)} ${item.unit || 'قطعة'}) = ${formatCurrency(item.lineTotal)}`,
    );
  });
  lines.push('------------------');
  lines.push(`المجموع الفرعي: ${formatCurrency(order.subtotal)}`);
  if (order.discountAmount > 0) {
    lines.push(`الخصم: - ${formatCurrency(order.discountAmount)}`);
  }
  lines.push(isPickup ? 'الاستلام من المتجر: مجانًا' : `التوصيل: ${formatCurrency(order.deliveryFee)}`);
  lines.push(`*الإجمالي النهائي: ${formatCurrency(order.totalAmount)}*`);
  if (storePhone.trim()) {
    lines.push(`📞 للتواصل: ${formatDigits(storePhone)}`);
  }
  return lines.join('\n');
}

export function openWhatsAppWithText(phone: string | null, message: string) {
  const encoded = encodeURIComponent(message);
  if (phone) {
    const normalized = normalizeWhatsAppPhone(phone);
    if (normalized.length >= 10) {
      window.open(`https://wa.me/${normalized}?text=${encoded}`, '_blank', 'noopener,noreferrer');
      return;
    }
  }
  window.open(`https://wa.me/?text=${encoded}`, '_blank', 'noopener,noreferrer');
}

/**
 * Renders the branded HTML receipt to an offscreen high-DPI canvas and sends ESC/POS raster bytes
 * via Web Bluetooth (GS v 0) — works on Chrome/Edge Desktop and Android browsers!
 */
export async function printOrderViaWebBluetooth(
  element: HTMLElement,
  paperWidth: ThermalPaperWidth,
): Promise<string> {
  const nav = navigator as Navigator & {
    bluetooth?: {
      requestDevice: (opts: {
        acceptAllDevices?: boolean;
        optionalServices?: (string | number)[];
      }) => Promise<{
        name?: string;
        gatt?: {
          connect: () => Promise<{
            getPrimaryServices: () => Promise<
              Array<{
                getCharacteristics: () => Promise<
                  Array<{
                    properties: { write?: boolean; writeWithoutResponse?: boolean };
                    writeValue?: (data: BufferSource) => Promise<void>;
                    writeValueWithoutResponse?: (data: BufferSource) => Promise<void>;
                  }>
                >;
              }>
            >;
          }>;
        };
      }>;
    };
  };

  if (!nav.bluetooth) {
    throw new Error('متصفحك لا يدعم الطباعة عبر Web Bluetooth. يمكنك استخدام زر "طباعة حرارية / PDF" القياسي.');
  }

  const widthPx = paperWidth === '58mm' ? 384 : 576;
  const device = await nav.bluetooth.requestDevice({
    acceptAllDevices: true,
    optionalServices: [
      '000018f0-0000-1000-8000-00805f9b34fb',
      '49535343-fe7d-4ae5-8fa9-9fafd205e455',
      'e7810a71-73ae-499d-8c15-faa9aef0c3f2',
      '0000ff00-0000-1000-8000-00805f9b34fb',
    ],
  });

  if (!device.gatt) {
    throw new Error('الطابعة المختارة لا تدعم اتصال GATT BLE. استخدم الطباعة النظامية.');
  }

  const server = await device.gatt.connect();
  const services = await server.getPrimaryServices();
  let writableChar: {
    properties: { write?: boolean; writeWithoutResponse?: boolean };
    writeValue?: (data: BufferSource) => Promise<void>;
    writeValueWithoutResponse?: (data: BufferSource) => Promise<void>;
  } | null = null;

  for (const svc of services) {
    const chars = await svc.getCharacteristics();
    for (const ch of chars) {
      if (ch.properties.writeWithoutResponse || ch.properties.write) {
        writableChar = ch;
        break;
      }
    }
    if (writableChar) break;
  }

  if (!writableChar) {
    throw new Error('لم يتم العثور على منفذ كتابة متوافق في الطابعة. استخدم الطباعة النظامية / PDF.');
  }

  // Render receipt canvas directly from element text for crisp Arabic raster bitmap
  const canvas = await renderReceiptTextCanvas(element, widthPx);
  const escPosBytes = canvasToEscPosRaster(canvas);

  const chunkSize = 180;
  for (let i = 0; i < escPosBytes.length; i += chunkSize) {
    const slice = escPosBytes.slice(i, i + chunkSize);
    if (writableChar.writeValueWithoutResponse) {
      await writableChar.writeValueWithoutResponse(slice);
    } else if (writableChar.writeValue) {
      await writableChar.writeValue(slice);
    }
    await new Promise((r) => setTimeout(r, 15));
  }

  return device.name || 'طابعة حرارية بلوتوث';
}

async function renderReceiptTextCanvas(element: HTMLElement, widthPx: number): Promise<HTMLCanvasElement> {
  const lines = (element.innerText || '')
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean);
  const lineHeight = widthPx === 384 ? 26 : 32;
  const fontSize = widthPx === 384 ? 18 : 22;
  const heightPx = Math.max(240, lines.length * lineHeight + 60);

  const canvas = document.createElement('canvas');
  canvas.width = widthPx;
  canvas.height = heightPx;
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, widthPx, heightPx);
  ctx.fillStyle = '#000000';
  ctx.direction = 'rtl';
  ctx.textAlign = 'right';
  ctx.font = `bold ${fontSize}px Tajawal, sans-serif`;

  let y = 36;
  for (const line of lines) {
    ctx.fillText(line, widthPx - 14, y, widthPx - 28);
    y += lineHeight;
  }
  return canvas;
}

function canvasToEscPosRaster(canvas: HTMLCanvasElement): Uint8Array {
  const width = canvas.width;
  const height = canvas.height;
  const ctx = canvas.getContext('2d')!;
  const imgData = ctx.getImageData(0, 0, width, height).data;
  const bytesPerRow = Math.ceil(width / 8);
  const chunks: number[] = [0x1b, 0x40, 0x1b, 0x61, 0x01]; // ESC @ + Center

  const sliceHeight = 160;
  for (let startY = 0; startY < height; startY += sliceHeight) {
    const chunkH = Math.min(sliceHeight, height - startY);
    chunks.push(
      0x1d,
      0x76,
      0x30,
      0x00,
      bytesPerRow & 0xff,
      (bytesPerRow >> 8) & 0xff,
      chunkH & 0xff,
      (chunkH >> 8) & 0xff,
    );
    for (let y = startY; y < startY + chunkH; y++) {
      for (let byteX = 0; byteX < bytesPerRow; byteX++) {
        let b = 0;
        for (let bit = 0; bit < 8; bit++) {
          const px = byteX * 8 + bit;
          if (px < width) {
            const idx = (y * width + px) * 4;
            const lum = 0.299 * imgData[idx] + 0.587 * imgData[idx + 1] + 0.114 * imgData[idx + 2];
            if (imgData[idx + 3] > 128 && lum < 175) {
              b |= 1 << (7 - bit);
            }
          }
        }
        chunks.push(b);
      }
    }
  }
  chunks.push(0x1b, 0x64, 0x04, 0x1d, 0x56, 0x42, 0x00);
  return new Uint8Array(chunks);
}

export function getReceiptSummaryData(order: Order, storeName: string) {
  const isPickup = order.fulfillmentType === 'pickup';
  return {
    storeName: storeName || 'الإيمان للتجارة',
    orderNumber: formatDigits(order.orderNumber || order.id.slice(0, 8)),
    date: formatDateTime(order.createdAt),
    fulfillmentLabel: isPickup ? 'الاستلام من المتجر' : 'توصيل للعنوان',
    statusLabel: orderStatusLabelFor(order.orderStatus, isPickup),
    paymentLabel: `${PAYMENT_METHOD_META[order.paymentMethod]?.label || order.paymentMethod} (${PAYMENT_STATUS_LABELS[order.paymentStatus] || ''})`,
    channelLabel: order.accountTypeAtOrder === 'wholesale' ? 'طلب جملة' : 'طلب متجر',
    addressLine: isPickup
      ? order.streetAddress || 'الفرع الرئيسي للمتجر'
      : [order.governorate, order.city, order.streetAddress].filter(Boolean).join(' - '),
  };
}
