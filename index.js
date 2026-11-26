require("dotenv").config();

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const express = require("express");
const pino = require("pino");
const QRCode = require("qrcode");
const qrcodeTerminal = require("qrcode-terminal");
const {
  default: makeWASocket,
  DisconnectReason,
  fetchLatestBaileysVersion,
  makeCacheableSignalKeyStore,
} = require("@whiskeysockets/baileys");
const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { useFirestoreAuthState } = require("./firestoreAuthState");

const BOT_VERSION = "2.0.0-baileys";
const DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID || "ai-studio-android-08e4f24d-823a-4020-a9a2-1cd3da356058";
const SERVICE_ACCOUNT_PATH = path.resolve(
  __dirname,
  process.env.FIREBASE_SERVICE_ACCOUNT_PATH || "./serviceAccountKey.json"
);
const PORT = Number(process.env.PORT || process.env.DASHBOARD_PORT || 3737);
const DEFAULT_COUNTRY_CODE = (process.env.DEFAULT_COUNTRY_CODE || "20").replace(/\D/g, "") || "20";
const DASHBOARD_PASSWORD = String(process.env.DASHBOARD_PASSWORD || "").trim();

function loadFirebaseServiceAccount() {
  const envJson = String(process.env.FIREBASE_SERVICE_ACCOUNT_JSON || "").trim();
  if (envJson) {
    try {
      return JSON.parse(envJson);
    } catch {
      try {
        const decoded = Buffer.from(envJson, "base64").toString("utf8");
        return JSON.parse(decoded);
      } catch (err) {
        console.error("[ERROR] Failed to parse FIREBASE_SERVICE_ACCOUNT_JSON:", err.message);
        process.exit(1);
      }
    }
  }

  if (fs.existsSync(SERVICE_ACCOUNT_PATH)) {
    try {
      return JSON.parse(fs.readFileSync(SERVICE_ACCOUNT_PATH, "utf8"));
    } catch (err) {
      console.error("[ERROR] Failed to read serviceAccountKey.json:", err.message);
      process.exit(1);
    }
  }

  console.error("============================================================");
  console.error("[ERROR] Firebase Service Account credentials not found!");
  console.error("Option A (Cloud / Render): Set FIREBASE_SERVICE_ACCOUNT_JSON env variable.");
  console.error("Option B (Local PC): Place serviceAccountKey.json inside whatsapp-bot/");
  console.error("============================================================");
  process.exit(1);
}

const serviceAccount = loadFirebaseServiceAccount();
const firebaseApp = initializeApp({
  credential: cert(serviceAccount),
});
const db = getFirestore(firebaseApp, DATABASE_ID);
const baileysLogger = pino({ level: "silent" });

// Live runtime state
let sock = null;
let authController = null;
let botState = "connecting"; // connecting | qr_ready | online | disconnected
let latestQrDataUrl = "";
let latestPairingCode = "";
let connectedPhone = "";
let connectedName = "";
let isProcessingOutbox = false;
let pendingQueueCount = 0;
let reconnectTimer = null;
let reconnectAttempts = 0;
let isStartingSocket = false;

let storeSettings = {
  storeName: "الإيمان للتجارة",
  whatsappBotEnabled: false,
  whatsappOtpEnabled: false,
  whatsappInvoiceEnabled: true,
  whatsappPromoEnabled: false,
  whatsappMinDelaySeconds: 20,
  whatsappDailyPromoLimit: 50,
};

let currentDayKey = getCairoDayKey();
const counters = {
  total: 0,
  otp: 0,
  invoice: 0,
  promo: 0,
  failed: 0,
};
const recentLogs = [];

function getCairoDayKey() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Africa/Cairo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}

function resetDailyCountersIfNeeded() {
  const today = getCairoDayKey();
  if (today !== currentDayKey) {
    currentDayKey = today;
    counters.total = 0;
    counters.otp = 0;
    counters.invoice = 0;
    counters.promo = 0;
    counters.failed = 0;
    addLog("تم تصفير عدادات اليوم الجديد تلقائياً (" + today + ")");
  }
}

function addLog(text) {
  const stamp = new Date().toLocaleTimeString("ar-EG", {
    timeZone: "Africa/Cairo",
    hour12: true,
  });
  const entry = `[${stamp}] ${text}`;
  console.log(entry);
  recentLogs.unshift(entry);
  if (recentLogs.length > 45) recentLogs.length = 45;
}

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function normalizeArabicDigits(raw) {
  return String(raw || "")
    .replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660))
    .replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0));
}

function toInternationalDigits(rawPhone) {
  let digits = normalizeArabicDigits(rawPhone).replace(/\D/g, "");
  if (!digits) return "";
  if (digits.startsWith("00")) {
    digits = digits.slice(2);
  }
  if (digits.startsWith("0") && digits.length === 11 && DEFAULT_COUNTRY_CODE === "20") {
    return "20" + digits.slice(1);
  }
  if (digits.length === 10 && digits.startsWith("1") && DEFAULT_COUNTRY_CODE === "20") {
    return "20" + digits;
  }
  if (digits.startsWith("0")) {
    return DEFAULT_COUNTRY_CODE + digits.slice(1);
  }
  return digits;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function publishHeartbeat() {
  resetDailyCountersIfNeeded();
  try {
    await db
      .collection("settings")
      .doc("whatsapp_bot_status")
      .set(
        {
          status: botState,
          connectedPhone,
          connectedName,
          sentTodayTotal: counters.total,
          sentTodayOtp: counters.otp,
          sentTodayInvoice: counters.invoice,
          sentTodayPromo: counters.promo,
          failedTodayCount: counters.failed,
          pendingQueueCount,
          botVersion: BOT_VERSION,
          lastHeartbeatAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
  } catch (err) {
    console.error("[Heartbeat Error]", err.message);
  }
}

async function resolveWhatsappJid(rawPhone) {
  if (!sock || botState !== "online") {
    throw new Error("البوت غير متصل بواتساب حالياً.");
  }
  const intlDigits = toInternationalDigits(rawPhone);
  if (!intlDigits || intlDigits.length < 10) {
    throw new Error("رقم الهاتف غير صالح.");
  }
  const results = await sock.onWhatsApp(intlDigits);
  const match = Array.isArray(results) ? results.find((r) => r && r.exists) : null;
  if (!match || !match.jid) {
    throw new Error("هذا الرقم غير مسجل على واتساب، يرجى مراجعة الرقم والمحاولة مرة أخرى.");
  }
  return match.jid;
}

// --- 1. Start / Reconnect Baileys Socket with Firestore Session Persistence ---
async function startWhatsappSocket() {
  if (isStartingSocket) return;
  isStartingSocket = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  try {
    botState = "connecting";
    latestQrDataUrl = "";
    await publishHeartbeat();

    authController = await useFirestoreAuthState(db, addLog);
    const { state, saveCreds } = authController;

    let version = [2, 3000, 1015901307];
    try {
      const fetched = await fetchLatestBaileysVersion();
      if (fetched && Array.isArray(fetched.version)) {
        version = fetched.version;
      }
    } catch (verErr) {
      addLog(`تنبيه: استخدام إصدار واتساب افتراضي (${verErr.message})`);
    }

    const nextSock = makeWASocket({
      version,
      logger: baileysLogger,
      auth: {
        creds: state.creds,
        keys: makeCacheableSignalKeyStore(state.keys, baileysLogger),
      },
      browser: ["Al-Eman Trade", "Chrome", "1.0.0"],
      printQRInTerminal: false,
      syncFullHistory: false,
      markOnlineOnConnect: false,
      connectTimeoutMs: 60000,
      defaultQueryTimeoutMs: 60000,
    });

    sock = nextSock;

    nextSock.ev.on("creds.update", () => {
      saveCreds();
    });

    nextSock.ev.on("connection.update", async (update) => {
      if (sock !== nextSock) return;
      const { connection, lastDisconnect, qr } = update;

      if (qr) {
        botState = "qr_ready";
        connectedPhone = "";
        connectedName = "";
        try {
          latestQrDataUrl = await QRCode.toDataURL(qr, { width: 320, margin: 2 });
        } catch (err) {
          addLog(`خطأ توليد صورة QR: ${err.message}`);
        }
        addLog("رمز QR جاهز — امسحه من CMD أو من صفحة لوحة التحكم، أو استخدم كود الربط بالهاتف.");
        qrcodeTerminal.generate(qr, { small: true });
        await publishHeartbeat();
      }

      if (connection === "open") {
        botState = "online";
        reconnectAttempts = 0;
        latestQrDataUrl = "";
        latestPairingCode = "";
        const me = nextSock.user || {};
        connectedPhone = String(me.id || "").split(":")[0].split("@")[0];
        connectedName = String(me.name || "");
        addLog(`✅ تم الاتصال بواتساب وحفظ الجلسة في Firestore (${connectedPhone})`);
        await publishHeartbeat();
        triggerOutboxProcessor();
        scanPendingOtpSessions();
      }

      if (connection === "close") {
        const statusCode =
          lastDisconnect?.error?.output?.statusCode ??
          lastDisconnect?.error?.statusCode ??
          0;
        const isLoggedOut = statusCode === DisconnectReason.loggedOut;
        const isBadSession = statusCode === DisconnectReason.badSession;

        botState = "disconnected";
        connectedPhone = "";
        connectedName = "";
        latestQrDataUrl = "";
        await publishHeartbeat();

        if (isLoggedOut || isBadSession) {
          addLog("⚠️ تم تسجيل الخروج من الهاتف أو انتهت الجلسة — جاري مسح الجلسة القديمة وتجهيز ربط جديد...");
          try {
            await authController?.clearAuthState();
          } catch (e) {
            addLog(`خطأ مسح الجلسة: ${e.message}`);
          }
          reconnectAttempts = 0;
          scheduleReconnect(2500);
        } else {
          reconnectAttempts += 1;
          const waitMs = Math.min(30000, 3000 * reconnectAttempts);
          addLog(`انقطع الاتصال مؤقتاً (كود ${statusCode}) — إعادة المحاولة خلال ${Math.round(waitMs / 1000)} ثانية...`);
          scheduleReconnect(waitMs);
        }
      }
    });
  } catch (err) {
    botState = "disconnected";
    addLog(`❌ تعذر تشغيل اتصال واتساب: ${err.message}`);
    await publishHeartbeat();
    reconnectAttempts += 1;
    scheduleReconnect(Math.min(30000, 5000 * reconnectAttempts));
  } finally {
    isStartingSocket = false;
  }
}

function scheduleReconnect(delayMs) {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    startWhatsappSocket();
  }, delayMs);
}

// --- 2. Real-time Store Settings Listener ---
db.collection("settings")
  .doc("store")
  .onSnapshot(
    (snap) => {
      if (!snap.exists) return;
      const d = snap.data() || {};
      storeSettings = {
        storeName: String(d.storeName || "الإيمان للتجارة").trim(),
        whatsappBotEnabled: Boolean(d.whatsappBotEnabled),
        whatsappOtpEnabled: Boolean(d.whatsappOtpEnabled),
        whatsappInvoiceEnabled: d.whatsappInvoiceEnabled !== false,
        whatsappPromoEnabled: Boolean(d.whatsappPromoEnabled),
        whatsappMinDelaySeconds: Math.max(10, Math.min(300, Number(d.whatsappMinDelaySeconds || 20))),
        whatsappDailyPromoLimit: Math.max(5, Math.min(500, Number(d.whatsappDailyPromoLimit || 50))),
      };
      addLog(
        `تحديث إعدادات المتجر: البوت=${storeSettings.whatsappBotEnabled ? "مفعّل" : "متوقف"} | OTP=${
          storeSettings.whatsappOtpEnabled ? "نعم" : "لا"
        } | الفواتير=${storeSettings.whatsappInvoiceEnabled ? "نعم" : "لا"} | العروض=${
          storeSettings.whatsappPromoEnabled ? "نعم" : "لا"
        }`
      );
      if (botState === "online") {
        triggerOutboxProcessor();
      }
    },
    (err) => {
      console.error("[Settings Listener Error]", err.message);
    }
  );

// --- 3. Zero-Trust WhatsApp OTP Session Handler ---
const activeOtpLocks = new Set();

async function handleOtpSessionDoc(docSnap) {
  const uid = docSnap.id;
  if (activeOtpLocks.has(uid)) return;
  const data = docSnap.data() || {};
  const status = data.status;

  if (status !== "request_send" && status !== "verify_attempt") return;
  if (botState !== "online" || !sock) return;

  activeOtpLocks.add(uid);
  const sessionRef = db.collection("whatsappOtpSessions").doc(uid);
  const secretRef = db.collection("whatsappOtpSecrets").doc(uid);

  try {
    resetDailyCountersIfNeeded();

    if (!storeSettings.whatsappBotEnabled || !storeSettings.whatsappOtpEnabled) {
      await sessionRef.set(
        {
          status: "failed",
          errorMessage: "خدمة التحقق عبر واتساب متوقفة مؤقتاً من إعدادات المتجر.",
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      return;
    }

    if (status === "request_send") {
      const phone = String(data.phone || "").trim();
      const prevSecretSnap = await secretRef.get();
      if (prevSecretSnap.exists) {
        const prevSentMs = prevSecretSnap.data().sentAtMs || 0;
        const diffSec = Math.floor((Date.now() - prevSentMs) / 1000);
        if (diffSec < 45) {
          await sessionRef.set(
            {
              status: "code_sent",
              errorMessage: `تم إرسال الكود للتو، يمكنك طلب كود جديد بعد ${45 - diffSec} ثانية.`,
              updatedAt: FieldValue.serverTimestamp(),
            },
            { merge: true }
          );
          return;
        }
      }

      const jid = await resolveWhatsappJid(phone);
      const otpCode = String(crypto.randomInt(100000, 1000000));
      const expiresAtMs = Date.now() + 5 * 60 * 1000;

      await secretRef.set({
        userId: uid,
        phone,
        code: otpCode,
        attempts: 0,
        maxAttempts: 5,
        sentAtMs: Date.now(),
        expiresAtMs,
        updatedAt: FieldValue.serverTimestamp(),
      });

      const otpText =
        `🔐 *${storeSettings.storeName}*\n\n` +
        `كود تأكيد رقم هاتفك هو:\n` +
        `*${otpCode}*\n\n` +
        `⏳ الكود صالح لمدة 5 دقائق فقط.\n` +
        `⚠️ لا تشارك هذا الكود مع أي شخص.`;

      await sock.sendMessage(jid, { text: otpText });

      counters.total += 1;
      counters.otp += 1;
      addLog(`✅ تم إرسال كود OTP إلى ${phone} (مستخدم ${uid.slice(0, 6)}...)`);

      await sessionRef.set(
        {
          status: "code_sent",
          errorMessage: FieldValue.delete(),
          attemptsRemaining: 5,
          sentAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
      await publishHeartbeat();
    } else if (status === "verify_attempt") {
      const submitted = normalizeArabicDigits(data.submittedCode || "").replace(/\D/g, "");
      const secretSnap = await secretRef.get();
      if (!secretSnap.exists) {
        await sessionRef.set(
          {
            status: "expired",
            errorMessage: "انتهت صلاحية الكود أو لم يتم طلب كود بعد. اضغط إعادة الإرسال.",
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
        return;
      }

      const secret = secretSnap.data();
      if (Date.now() > (secret.expiresAtMs || 0)) {
        await secretRef.delete();
        await sessionRef.set(
          {
            status: "expired",
            errorMessage: "انتهت صلاحية الكود (مر أكثر من 5 دقائق). اضغط إعادة الإرسال.",
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
        return;
      }

      const nextAttempts = Number(secret.attempts || 0) + 1;
      const maxAttempts = Number(secret.maxAttempts || 5);

      if (submitted && submitted === String(secret.code)) {
        const verifiedPhone = String(secret.phone || data.phone || "").trim();
        await db
          .collection("users")
          .doc(uid)
          .set(
            {
              phone: verifiedPhone,
              phoneVerified: true,
              phoneVerifiedAt: FieldValue.serverTimestamp(),
              updatedAt: FieldValue.serverTimestamp(),
            },
            { merge: true }
          );

        await sessionRef.set(
          {
            status: "verified",
            submittedCode: "",
            errorMessage: FieldValue.delete(),
            verifiedAt: FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
        await secretRef.delete();
        addLog(`🎉 تم تأكيد رقم هاتف العميل ${verifiedPhone} بنجاح!`);
      } else if (nextAttempts >= maxAttempts) {
        await secretRef.delete();
        await sessionRef.set(
          {
            status: "expired",
            attemptsRemaining: 0,
            errorMessage: "تم تجاوز الحد الأقصى للمحاولات الخاطئة. يرجى طلب كود جديد.",
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      } else {
        const remaining = maxAttempts - nextAttempts;
        await secretRef.update({
          attempts: nextAttempts,
          updatedAt: FieldValue.serverTimestamp(),
        });
        await sessionRef.set(
          {
            status: "invalid_code",
            attemptsRemaining: remaining,
            errorMessage: `كود التحقق غير صحيح (متبقي ${remaining} محاولات).`,
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      }
    }
  } catch (err) {
    counters.failed += 1;
    addLog(`❌ فشل OTP (${uid.slice(0, 6)}): ${err.message}`);
    await sessionRef.set(
      {
        status: "failed",
        errorMessage: String(err.message || "تعذر إرسال كود التحقق عبر واتساب.").slice(0, 280),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
    await publishHeartbeat();
  } finally {
    activeOtpLocks.delete(uid);
  }
}

async function scanPendingOtpSessions() {
  try {
    const snap = await db
      .collection("whatsappOtpSessions")
      .where("status", "in", ["request_send", "verify_attempt"])
      .get();
    snap.forEach((docSnap) => {
      handleOtpSessionDoc(docSnap);
    });
  } catch (err) {
    console.error("[OTP Scan Error]", err.message);
  }
}

db.collection("whatsappOtpSessions")
  .where("status", "in", ["request_send", "verify_attempt"])
  .onSnapshot(
    (snapshot) => {
      snapshot.docChanges().forEach((change) => {
        if (change.type === "added" || change.type === "modified") {
          handleOtpSessionDoc(change.doc);
        }
      });
    },
    (err) => {
      console.error("[OTP Listener Error]", err.message);
    }
  );

// --- 4. Priority Outbox Queue Processor (Invoices + Anti-Ban Paced Promos) ---
let latestPendingOutboxDocs = [];

db.collection("whatsappOutbox")
  .where("status", "==", "pending")
  .onSnapshot(
    (snapshot) => {
      latestPendingOutboxDocs = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
      pendingQueueCount = latestPendingOutboxDocs.length;
      if (botState === "online" && pendingQueueCount > 0) {
        triggerOutboxProcessor();
      }
    },
    (err) => {
      console.error("[Outbox Listener Error]", err.message);
    }
  );

function triggerOutboxProcessor() {
  if (isProcessingOutbox) return;
  processOutboxLoop().catch((err) => {
    console.error("[Outbox Loop Error]", err.message);
  });
}

async function processOutboxLoop() {
  if (isProcessingOutbox) return;
  isProcessingOutbox = true;

  try {
    while (botState === "online" && sock && latestPendingOutboxDocs.length > 0) {
      resetDailyCountersIfNeeded();

      if (!storeSettings.whatsappBotEnabled) {
        break;
      }

      const sorted = [...latestPendingOutboxDocs].sort((a, b) => {
        const pa = Number(a.priority || 2);
        const pb = Number(b.priority || 2);
        if (pa !== pb) return pa - pb;
        const ta = a.createdAt?.toMillis ? a.createdAt.toMillis() : 0;
        const tb = b.createdAt?.toMillis ? b.createdAt.toMillis() : 0;
        return ta - tb;
      });

      const nextJob = sorted.find((job) => {
        if (job.type === "invoice") return storeSettings.whatsappInvoiceEnabled;
        if (job.type === "promo") {
          return (
            storeSettings.whatsappPromoEnabled &&
            counters.promo < storeSettings.whatsappDailyPromoLimit
          );
        }
        return true;
      });

      if (!nextJob) {
        break;
      }

      const jobRef = db.collection("whatsappOutbox").doc(nextJob.id);

      const claimed = await db.runTransaction(async (tx) => {
        const snap = await tx.get(jobRef);
        if (!snap.exists || snap.data().status !== "pending") return false;
        tx.update(jobRef, {
          status: "processing",
          updatedAt: FieldValue.serverTimestamp(),
        });
        return true;
      });

      if (!claimed) {
        await sleep(300);
        continue;
      }

      try {
        const jid = await resolveWhatsappJid(nextJob.targetPhone);
        const messageText = String(nextJob.message || "");

        // Anti-ban human-like typing simulation for promotional campaigns
        if (nextJob.type === "promo") {
          try {
            await sock.sendPresenceUpdate("composing", jid);
            const typingMs = Math.min(5500, Math.max(1800, messageText.length * 22)) + crypto.randomInt(300, 1400);
            await sleep(typingMs);
            await sock.sendPresenceUpdate("paused", jid);
            await sleep(crypto.randomInt(250, 700));
          } catch (_) {
            // Presence update is best-effort; never block sending
          }
        }

        await sock.sendMessage(jid, { text: messageText });

        counters.total += 1;
        if (nextJob.type === "invoice") counters.invoice += 1;
        else if (nextJob.type === "promo") counters.promo += 1;

        await jobRef.update({
          status: "sent",
          errorMessage: FieldValue.delete(),
          sentAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        });

        addLog(
          `📨 تم إرسال (${nextJob.type === "invoice" ? "فاتورة" : "عرض"}) إلى ${nextJob.targetPhone}`
        );
        await publishHeartbeat();

        if (nextJob.type === "promo") {
          const minSec = storeSettings.whatsappMinDelaySeconds;
          const jitterSec = crypto.randomInt(0, Math.max(6, Math.floor(minSec * 0.75)));
          const isBatchBreak = counters.promo > 0 && counters.promo % 15 === 0;
          const batchCooldownSec = isBatchBreak ? crypto.randomInt(90, 165) : 0;
          const waitSec = minSec + jitterSec + batchCooldownSec;
          addLog(
            isBatchBreak
              ? `☕ استراحة أمان تلقائية بعد ${counters.promo} رسالة (${waitSec} ثانية) لحماية الرقم من الحظر...`
              : `⏱️ انتظار آمن (${waitSec} ثانية) قبل رسالة العرض التالية لحماية الرقم...`
          );
          await sleep(waitSec * 1000);
        } else {
          await sleep(2500);
        }
      } catch (sendErr) {
        counters.failed += 1;
        const nextRetry = Number(nextJob.retryCount || 0) + 1;
        const msg = String(sendErr.message || "تعذر إرسال الرسالة عبر واتساب.").slice(0, 280);
        const isNotOnWhatsapp = msg.includes("غير مسجل على واتساب") || msg.includes("غير صالح");
        const finalStatus = isNotOnWhatsapp || nextRetry >= 3 ? "failed" : "pending";

        await jobRef.update({
          status: finalStatus,
          retryCount: nextRetry,
          errorMessage: msg,
          updatedAt: FieldValue.serverTimestamp(),
        });

        addLog(`❌ فشل إرسال (${nextJob.targetPhone}): ${msg}`);
        await publishHeartbeat();
        await sleep(3000);
      }
    }
  } finally {
    isProcessingOutbox = false;
  }
}

// --- 5. Dashboard Cookie Auth Helper (Optional Password Protection for Cloud) ---
const SESSION_COOKIE_NAME = "aleman_bot_sess";
const sessionSecret = crypto
  .createHash("sha256")
  .update((DASHBOARD_PASSWORD || "local-open") + "::" + (serviceAccount.private_key_id || "aleman"))
  .digest("hex");

function computeSessionToken() {
  return crypto
    .createHmac("sha256", sessionSecret)
    .update("authorized:" + DASHBOARD_PASSWORD)
    .digest("hex");
}

function parseCookies(req) {
  const header = String(req.headers.cookie || "");
  const out = {};
  header.split(";").forEach((part) => {
    const idx = part.indexOf("=");
    if (idx > 0) {
      const k = part.slice(0, idx).trim();
      const v = decodeURIComponent(part.slice(idx + 1).trim());
      out[k] = v;
    }
  });
  return out;
}

function timingSafeEqualStr(a, b) {
  const bufA = Buffer.from(String(a || ""));
  const bufB = Buffer.from(String(b || ""));
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

function isDashboardAuthenticated(req) {
  if (!DASHBOARD_PASSWORD) return true;
  const cookies = parseCookies(req);
  const token = cookies[SESSION_COOKIE_NAME] || "";
  return timingSafeEqualStr(token, computeSessionToken());
}

function requireDashboardAuth(req, res, next) {
  if (isDashboardAuthenticated(req)) return next();
  if (req.path.startsWith("/api/")) {
    return res.status(401).json({ ok: false, error: "يرجى تسجيل الدخول أولاً." });
  }
  return res.redirect("/login");
}

// --- 6. Express Web Dashboard & UptimeRobot Health Endpoint ---
const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// Open health check for Render & UptimeRobot (every 5 min keeps Render Free awake 24/7!)
app.get("/health", (_req, res) => {
  res.status(200).json({
    ok: true,
    status: botState,
    version: BOT_VERSION,
  });
});

app.get("/login", (req, res) => {
  if (!DASHBOARD_PASSWORD || isDashboardAuthenticated(req)) {
    return res.redirect("/");
  }
  const hasError = req.query.err === "1";
  res.send(`<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>تسجيل الدخول - بوت الإيمان للتجارة</title>
  <style>
    body { font-family: Tahoma, Arial, sans-serif; background: #f5f6fa; color: #181a20; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 16px; }
    .box { background: #fff; border-radius: 16px; padding: 24px; max-width: 380px; width: 100%; box-shadow: 0 4px 16px rgba(0,0,0,0.08); text-align: center; }
    h2 { margin: 0 0 8px; }
    p { color: #555; font-size: 14px; margin: 0 0 16px; }
    input { width: 100%; box-sizing: border-box; padding: 12px; border: 1px solid #d1d5db; border-radius: 10px; font-size: 15px; margin-bottom: 12px; text-align: center; }
    button { width: 100%; padding: 12px; border: none; border-radius: 10px; background: #ffd000; color: #181a20; font-weight: bold; font-size: 15px; cursor: pointer; }
    .err { background: #fee2e2; color: #991b1b; padding: 8px 12px; border-radius: 8px; font-size: 13px; margin-bottom: 12px; }
  </style>
</head>
<body>
  <div class="box">
    <h2>🔐 لوحة بوت واتساب</h2>
    <p>أدخل كلمة سر لوحة التحكم للمتابعة</p>
    ${hasError ? '<div class="err">كلمة السر غير صحيحة، حاول مرة أخرى.</div>' : ""}
    <form method="POST" action="/login">
      <input type="password" name="password" placeholder="كلمة السر" required autofocus />
      <button type="submit">دخول</button>
    </form>
  </div>
</body>
</html>`);
});

app.post("/login", (req, res) => {
  if (!DASHBOARD_PASSWORD) return res.redirect("/");
  const submitted = String(req.body?.password || "").trim();
  if (timingSafeEqualStr(submitted, DASHBOARD_PASSWORD)) {
    res.setHeader(
      "Set-Cookie",
      `${SESSION_COOKIE_NAME}=${encodeURIComponent(computeSessionToken())}; Path=/; HttpOnly; SameSite=Lax; Max-Age=2592000`
    );
    return res.redirect("/");
  }
  return res.redirect("/login?err=1");
});

app.get("/api/status", requireDashboardAuth, (_req, res) => {
  resetDailyCountersIfNeeded();
  res.json({
    botState,
    connectedPhone,
    connectedName,
    counters,
    pendingQueueCount,
    storeSettings,
    hasQr: Boolean(latestQrDataUrl),
    latestPairingCode,
    version: BOT_VERSION,
  });
});

app.post("/api/pair", requireDashboardAuth, async (req, res) => {
  try {
    if (botState === "online") {
      return res.json({ ok: false, error: "البوت متصل بالفعل بواتساب!" });
    }
    if (!sock) {
      return res.json({ ok: false, error: "جاري تجهيز الاتصال، حاول بعد ثوانٍ..." });
    }
    const rawPhone = String(req.body?.phone || "").trim();
    const intlDigits = toInternationalDigits(rawPhone);
    if (!intlDigits || intlDigits.length < 10) {
      return res.json({
        ok: false,
        error: "يرجى إدخال رقم واتساب صحيح (مثال: 01012345678).",
      });
    }
    const rawCode = await sock.requestPairingCode(intlDigits);
    const formatted =
      rawCode && rawCode.length === 8
        ? `${rawCode.slice(0, 4)}-${rawCode.slice(4)}`
        : String(rawCode || "");
    latestPairingCode = formatted;
    addLog(`🔑 تم إنشاء كود ربط بالهاتف للرقم ${intlDigits}: ${formatted}`);
    return res.json({ ok: true, code: formatted, phone: intlDigits });
  } catch (err) {
    addLog(`❌ فشل إنشاء كود الربط: ${err.message}`);
    return res.json({
      ok: false,
      error: "تعذر إنشاء كود الربط الآن — تأكد من الرقم أو استخدم رمز QR.",
    });
  }
});

app.post("/api/logout", requireDashboardAuth, async (_req, res) => {
  try {
    addLog("طلب تسجيل خروج وإعادة ربط رقم جديد من لوحة التحكم...");
    try {
      await sock?.logout();
    } catch {
      // ignore if already disconnected
    }
    try {
      await authController?.clearAuthState();
    } catch {
      // ignore
    }
    botState = "disconnected";
    connectedPhone = "";
    connectedName = "";
    latestQrDataUrl = "";
    latestPairingCode = "";
    await publishHeartbeat();
    scheduleReconnect(1500);
    return res.json({ ok: true });
  } catch (err) {
    return res.status(500).json({ ok: false, error: err.message });
  }
});

app.get("/", requireDashboardAuth, (_req, res) => {
  resetDailyCountersIfNeeded();
  const statusBadge =
    botState === "online"
      ? `<span style="background:#16a34a;color:#fff;padding:6px 14px;border-radius:999px;font-weight:bold;">🟢 متصل وجاهز للإرسال</span>`
      : botState === "qr_ready"
      ? `<span style="background:#f59e0b;color:#111;padding:6px 14px;border-radius:999px;font-weight:bold;">🟡 بانتظار ربط الواتساب (QR أو كود)</span>`
      : `<span style="background:#dc2626;color:#fff;padding:6px 14px;border-radius:999px;font-weight:bold;">🔴 جاري الاتصال / غير متصل</span>`;

  const pairingSection =
    botState !== "online"
      ? `<div class="card">
          <h3 style="margin-top:0;">ربط رقم واتساب المتجر (اختر الطريقة الأسهل لك)</h3>
          <div class="pair-grid">
            <div class="pair-col">
              <h4>الطريقة 1: مسح رمز QR</h4>
              ${
                latestQrDataUrl
                  ? `<img src="${latestQrDataUrl}" alt="WhatsApp QR Code" style="width:240px;height:240px;border:4px solid #ffd000;border-radius:14px;background:#fff;" />
                     <p class="hint">افتح واتساب ← الأجهزة المرتبطة ← ربط جهاز ← امسح الرمز.</p>`
                  : `<p class="hint">جاري تجهيز رمز QR... سيتم التحديث تلقائياً.</p>`
              }
            </div>
            <div class="pair-col">
              <h4>الطريقة 2: الربط بكود (بدون كاميرا!)</h4>
              <p class="hint">مناسبة لو فاتح الصفحة من نفس الموبايل اللي عليه واتساب المحل:</p>
              <input id="pairPhone" type="tel" placeholder="مثال: 01012345678" style="width:100%;box-sizing:border-box;padding:11px;border:1px solid #d1d5db;border-radius:10px;font-size:15px;text-align:center;margin-bottom:10px;" />
              <button onclick="requestPairCode()" style="width:100%;padding:11px;border:none;border-radius:10px;background:#ffd000;color:#181a20;font-weight:bold;font-size:15px;cursor:pointer;">إنشاء كود الربط (8 حروف)</button>
              <div id="pairResult" style="margin-top:12px;">
                ${
                  latestPairingCode
                    ? `<div class="code-box">${escapeHtml(latestPairingCode)}</div>
                       <p class="hint">في واتساب: الأجهزة المرتبطة ← ربط جهاز ← الربط برقم الهاتف بدلاً من ذلك ← أدخل هذا الكود.</p>`
                    : ""
                }
              </div>
            </div>
          </div>
        </div>`
      : `<div class="card" style="text-align:center;">
          <p style="margin:0 0 10px;font-size:16px;">✅ مربوط بالرقم: <strong>${escapeHtml(connectedPhone || "—")}</strong> ${
          connectedName ? `(${escapeHtml(connectedName)})` : ""
        }</p>
          <p class="hint" style="margin:0 0 12px;">الجلسة محفوظة تلقائياً داخل Firebase Firestore — حتى لو أُعيد تشغيل السيرفر سيعود البوت متصلاً بدون QR.</p>
          <button onclick="logoutBot()" style="padding:9px 16px;border:1px solid #dc2626;border-radius:10px;background:#fff;color:#dc2626;font-weight:bold;cursor:pointer;">فصل الواتساب وربط رقم آخر</button>
        </div>`;

  const logsHtml = recentLogs
    .map((line) => `<div style="padding:4px 0;border-bottom:1px solid #eee;font-size:13px;">${escapeHtml(line)}</div>`)
    .join("");

  res.send(`<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>بوت واتساب - ${escapeHtml(storeSettings.storeName)}</title>
  <style>
    body { font-family: Tahoma, Arial, sans-serif; background: #f5f6fa; color: #181a20; margin: 0; padding: 20px; }
    .container { max-width: 780px; margin: 0 auto; }
    .card { background: #fff; border-radius: 16px; padding: 18px; margin-bottom: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; }
    .stat { background: #f8f9fc; border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px; text-align: center; }
    .stat strong { display: block; font-size: 22px; margin-top: 4px; }
    .pair-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
    .pair-col { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; padding: 14px; text-align: center; }
    .pair-col h4 { margin: 0 0 10px; }
    .hint { color: #555; font-size: 13px; line-height: 1.6; }
    .code-box { font-size: 26px; font-weight: bold; letter-spacing: 3px; background: #181a20; color: #ffd000; padding: 12px; border-radius: 10px; direction: ltr; margin: 8px 0; }
  </style>
</head>
<body>
  <div class="container">
    <div class="card" style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;">
      <div>
        <h2 style="margin:0 0 4px;">🤖 بوت واتساب — ${escapeHtml(storeSettings.storeName)}</h2>
        <div style="color:#666;font-size:13px;">محرك Baileys الخفيف + حفظ الجلسة في Firestore • الإصدار ${BOT_VERSION}</div>
      </div>
      <div>${statusBadge}</div>
    </div>

    ${pairingSection}

    <div class="card">
      <h3 style="margin-top:0;">📊 إحصائيات اليوم</h3>
      <div class="grid">
        <div class="stat">أكواد OTP<strong>${counters.otp}</strong></div>
        <div class="stat">فواتير الطلبات<strong>${counters.invoice}</strong></div>
        <div class="stat">رسائل العروض<strong>${counters.promo} / ${storeSettings.whatsappDailyPromoLimit}</strong></div>
        <div class="stat">قيد الانتظار<strong>${pendingQueueCount}</strong></div>
      </div>
    </div>

    <div class="card">
      <h3 style="margin-top:0;">⚙️ حالة الخدمات (من إعدادات التطبيق)</h3>
      <div style="display:flex;gap:10px;flex-wrap:wrap;font-size:14px;">
        <span>المفتاح الرئيسي: <b>${storeSettings.whatsappBotEnabled ? "مفعّل ✅" : "متوقف ⛔"}</b></span> |
        <span>OTP التسجيل: <b>${storeSettings.whatsappOtpEnabled ? "مفعّل" : "متوقف"}</b></span> |
        <span>فواتير تلقائية: <b>${storeSettings.whatsappInvoiceEnabled ? "مفعّلة" : "متوقفة"}</b></span> |
        <span>العروض: <b>${storeSettings.whatsappPromoEnabled ? "مفعّلة" : "متوقفة"}</b></span>
      </div>
    </div>

    <div class="card">
      <h3 style="margin-top:0;">📝 آخر الأحداث</h3>
      <div>${logsHtml || "<p style='color:#777'>لا توجد أحداث بعد.</p>"}</div>
    </div>
  </div>
  <script>
    async function requestPairCode() {
      const phone = document.getElementById('pairPhone').value.trim();
      const box = document.getElementById('pairResult');
      if (!phone) {
        box.innerHTML = '<div style="color:#dc2626;font-size:13px;">أدخل رقم الواتساب أولاً.</div>';
        return;
      }
      box.innerHTML = '<div style="color:#555;font-size:13px;">جاري إنشاء كود الربط...</div>';
      try {
        const res = await fetch('/api/pair', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ phone })
        });
        const data = await res.json();
        if (data.ok) {
          box.innerHTML = '<div class="code-box">' + data.code + '</div><p class="hint">افتح واتساب ← الأجهزة المرتبطة ← ربط جهاز ← الربط برقم الهاتف بدلاً من ذلك ← اكتب الكود أعلاه.</p>';
        } else {
          box.innerHTML = '<div style="color:#dc2626;font-size:13px;">' + (data.error || 'تعذر إنشاء الكود') + '</div>';
        }
      } catch (e) {
        box.innerHTML = '<div style="color:#dc2626;font-size:13px;">خطأ اتصال بالخادم.</div>';
      }
    }

    async function logoutBot() {
      if (!confirm('هل تريد فصل رقم الواتساب الحالي وربط رقم جديد؟')) return;
      await fetch('/api/logout', { method: 'POST' });
      setTimeout(() => location.reload(), 1800);
    }

    // تحديث تلقائي كل 12 ثانية فقط إذا لم يكن المستخدم يكتب في خانة الهاتف
    setInterval(() => {
      const active = document.activeElement;
      if (active && active.id === 'pairPhone') return;
      location.reload();
    }, 12000);
  </script>
</body>
</html>`);
});

app.listen(PORT, "0.0.0.0", () => {
  addLog(`🌐 لوحة التحكم تعمل على المنفذ ${PORT} (Health: /health)`);
  if (!DASHBOARD_PASSWORD) {
    addLog(`ℹ️ الوضع المحلي: افتح http://localhost:${PORT} للربط عبر QR أو كود الهاتف.`);
  }
});

// 30-second live heartbeat for Android Admin panel
setInterval(() => {
  publishHeartbeat();
}, 30 * 1000);

publishHeartbeat();
startWhatsappSocket();
