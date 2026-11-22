const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const OWNER_UID = "owner_789";
const PROD_MGR_UID = "prod_mgr_1";
const INV_MGR_UID = "inv_mgr_1";
const OWNER_EMAIL = "mohamedamerghazy@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write users or addresses", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("addresses").get());
});

test("Authenticated user: can create valid retail profile and update own phone", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      phone: "01001234567",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phone: "01009998888",
      updatedAt: pastDate(),
    })
  );
});

test("Security: user cannot self-assign admin role or self-approve wholesale", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "owner",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "wholesale",
      wholesaleStatus: "approved",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Security: user can submit pending wholesale request, cannot approve it themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountType: "wholesale",
      wholesaleStatus: "pending",
      wholesaleCompanyName: "شركة النور للتجارة",
      updatedAt: pastDate(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wholesaleStatus: "approved",
      updatedAt: pastDate(),
    })
  );
});

test("Security: cross-user PII isolation and list rejection for non-admin", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      fullName: "محمود علي",
      email: "bob@example.com",
      phone: "01111111111",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Security: spoofed unverified admin email cannot list users or approve wholesale", async () => {
  const spoofDb = testEnv.authenticatedContext("attacker_1", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(spoofDb.collection("users").get());
});

test("Owner: verified bootstrapped owner can list users and approve wholesale", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "wholesale",
      wholesaleStatus: "pending",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(ownerDb.collection("users").get());
  await assertSucceeds(
    ownerDb.collection("users").doc(ALICE_UID).update({
      wholesaleStatus: "approved",
      updatedAt: pastDate(),
    })
  );
});

test("Addresses subcollection: owner CRUD succeeds, cross-user access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").set({
      userId: ALICE_UID,
      label: "المحل",
      recipientName: "أحمد محمد",
      phone: "01001234567",
      governorate: "القاهرة",
      city: "مدينة نصر",
      streetAddress: "15 شارع عباس العقاد",
      isDefault: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("addresses").get()
  );
});

// --- Phase 3 Tests: Staff RBAC, Categories, and Products ---

test("Staff RBAC: owner creates staff with product_manager permissions; non-staff cannot write staff", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const ts = pastDate();
  await assertSucceeds(
    ownerDb.collection("staff").doc(PROD_MGR_UID).set({
      uid: PROD_MGR_UID,
      fullName: "كريم سامي",
      email: "karim@aleman.com",
      role: "product_manager",
      permissions: ["manage_products", "manage_categories", "manage_offers", "manage_banners"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular customer cannot create or list staff
  await assertFails(aliceDb.collection("staff").get());
  await assertFails(
    aliceDb.collection("staff").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد",
      email: "a@a.com",
      role: "manager",
      permissions: ["manage_products"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Categories & Products: guest reads active catalog only, cannot write; product_manager can manage catalog", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("staff").doc(PROD_MGR_UID).set({
      uid: PROD_MGR_UID,
      fullName: "كريم سامي",
      email: "karim@aleman.com",
      role: "product_manager",
      permissions: ["manage_products", "manage_categories"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("categories").doc("cat_hidden").set({
      name: "قسم مخفي",
      displayOrder: 2,
      isActive: false,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const prodMgrDb = testEnv.authenticatedContext(PROD_MGR_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();

  // Product Manager creates active category and product
  await assertSucceeds(
    prodMgrDb.collection("categories").doc("cat_food").set({
      name: "المواد الغذائية",
      displayOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    prodMgrDb.collection("products").doc("prod_oil").set({
      name: "زيت طعام 1 لتر",
      description: "زيت نباتي نقي عبوة 1 لتر",
      categoryId: "cat_food",
      sku: "OIL-1L",
      barcode: "6223000111222",
      qrCode: "ALEMAN-QR-OIL-1L",
      imageUrls: [],
      retailPrice: 85.0,
      retailDiscountPrice: 78.0,
      wholesalePrice: 70.0,
      wholesaleMinQty: 12,
      unit: "زجاجة",
      stockQuantity: 120,
      lowStockThreshold: 10,
      isFeatured: true,
      isOffer: true,
      isActive: true,
      salesCount: 0,
      averageRating: 0.0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest can query active categories and products, but unfiltered list or hidden doc read fails
  await assertSucceeds(unauthDb.collection("categories").where("isActive", "==", true).get());
  await assertSucceeds(unauthDb.collection("products").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("categories").doc("cat_hidden").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(
    unauthDb.collection("categories").doc("cat_hack").set({
      name: "اختراق",
      displayOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Products RBAC: inventory_manager can update stock only, cannot alter prices; invalid discount rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("staff").doc(INV_MGR_UID).set({
      uid: INV_MGR_UID,
      fullName: "حسام مخزون",
      email: "inv@aleman.com",
      role: "inventory_manager",
      permissions: ["manage_inventory"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("products").doc("prod_rice").set({
      name: "أرز مصري 5 كجم",
      description: "أرز فاخر حبة قصيرة",
      categoryId: "cat_food",
      sku: "RICE-5K",
      imageUrls: [],
      retailPrice: 175.0,
      wholesalePrice: 155.0,
      wholesaleMinQty: 10,
      unit: "شكارة",
      stockQuantity: 50,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      salesCount: 0,
      averageRating: 0.0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const invDb = testEnv.authenticatedContext(INV_MGR_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  // Inventory manager can update stockQuantity
  await assertSucceeds(
    invDb.collection("products").doc("prod_rice").update({
      stockQuantity: 42,
      updatedAt: pastDate(),
    })
  );

  // Inventory manager CANNOT update retailPrice or delete product
  await assertFails(
    invDb.collection("products").doc("prod_rice").update({
      retailPrice: 10.0,
      updatedAt: pastDate(),
    })
  );
  await assertFails(invDb.collection("products").doc("prod_rice").delete());

  // Owner cannot set discount price >= retailPrice
  await assertFails(
    ownerDb.collection("products").doc("prod_rice").update({
      retailDiscountPrice: 200.0,
      updatedAt: pastDate(),
    })
  );

  // Owner/product_manager CAN update product with multiple imageUrls
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_rice").update({
      imageUrls: [
        "https://res.cloudinary.com/demo/image/upload/v1/aleman/products/p1.webp",
        "https://res.cloudinary.com/demo/image/upload/v2/aleman/products/p2.webp",
        "https://res.cloudinary.com/demo/image/upload/v3/aleman/products/p3.webp",
      ],
      updatedAt: pastDate(),
    })
  );
});

// --- Phase 5 Tests: Cart Isolation, Atomic Checkout Orders, Coupons, Delivery Zones ---

test("Cart isolation: user manages own cart only; cross-user and unauthenticated cart access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_oil").set({
      userId: ALICE_UID,
      productId: "prod_oil",
      quantity: 4,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_oil").update({
      quantity: 8,
      updatedAt: pastDate(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_oil").set({
      userId: ALICE_UID,
      productId: "prod_oil",
      quantity: 1,
      updatedAt: ts,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cart").get());
});

test("Orders & Atomic Checkout: atomic order decrements stock & increments coupon; spoofing & standalone stock tampering fail", async () => {
  const ORDER_MGR_UID = "order_mgr_1";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("staff").doc(ORDER_MGR_UID).set({
      uid: ORDER_MGR_UID,
      fullName: "سامي طلبات",
      email: "orders@aleman.com",
      role: "order_manager",
      permissions: ["manage_orders", "manage_delivery"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("products").doc("prod_tea").set({
      name: "شاي 250 جم",
      description: "شاي فاخر",
      categoryId: "cat_food",
      sku: "TEA-250",
      imageUrls: [],
      retailPrice: 60.0,
      wholesalePrice: 48.0,
      wholesaleMinQty: 12,
      unit: "عبوة",
      stockQuantity: 30,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      salesCount: 2,
      averageRating: 0.0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("coupons").doc("cpn_10").set({
      code: "ALEMAN10",
      discountType: "percent",
      discountValue: 10.0,
      minOrderAmount: 100.0,
      usedCount: 0,
      usageLimit: 5,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const orderMgrDb = testEnv.authenticatedContext(ORDER_MGR_UID).firestore();
  const ts = pastDate();

  // Retail user CANNOT spoof accountTypeAtOrder = 'wholesale' or pre-verified statuses
  await assertFails(
    aliceDb.collection("orders").doc("spoof_order").set({
      userId: ALICE_UID,
      orderNumber: "EM-SPOOF1",
      customerName: "أحمد محمد",
      customerEmail: "alice@example.com",
      accountTypeAtOrder: "wholesale",
      items: [{ productId: "prod_tea", productName: "شاي 250 جم", sku: "TEA-250", unit: "عبوة", pricingMode: "wholesale", unitPrice: 48.0, quantity: 12, lineTotal: 576.0 }],
      subtotal: 576.0,
      discountAmount: 0.0,
      deliveryFee: 0.0,
      totalAmount: 576.0,
      paymentMethod: "cod",
      paymentStatus: "pending_review",
      orderStatus: "new_order",
      deliveryLabel: "المنزل",
      recipientName: "أحمد محمد",
      phone: "01001234567",
      governorate: "القاهرة",
      city: "مدينة نصر",
      streetAddress: "10 شارع النصر",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Customer CANNOT tamper with product stockQuantity directly without an atomic order transaction
  await assertFails(
    aliceDb.collection("products").doc("prod_tea").update({
      stockQuantity: 10,
      salesCount: 22,
      updatedAt: ts,
    })
  );

  // Valid atomic checkout transaction via runTransaction with standard date timestamp
  const serverTs = pastDate();
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const orderRef = aliceDb.collection("orders").doc("order_atomic_1");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      const prodRef = aliceDb.collection("products").doc("prod_tea");
      const couponRef = aliceDb.collection("coupons").doc("cpn_10");

      const prodSnap = await tx.get(prodRef);
      const couponSnap = await tx.get(couponRef);

      tx.set(orderRef, {
        userId: ALICE_UID,
        orderNumber: "EM-10001",
        customerName: "أحمد محمد",
        customerEmail: "alice@example.com",
        accountTypeAtOrder: "retail",
        items: [{
          productId: "prod_tea",
          productName: "شاي 250 جم",
          sku: "TEA-250",
          unit: "عبوة",
          pricingMode: "retail",
          unitPrice: 60.0,
          quantity: 5,
          lineTotal: 300.0,
        }],
        subtotal: 300.0,
        discountAmount: 30.0,
        couponCode: "ALEMAN10",
        deliveryFee: 20.0,
        totalAmount: 290.0,
        paymentMethod: "cod",
        paymentStatus: "pending_review",
        orderStatus: "new_order",
        deliveryLabel: "المنزل",
        recipientName: "أحمد محمد",
        phone: "01001234567",
        governorate: "القاهرة",
        city: "مدينة نصر",
        streetAddress: "10 شارع النصر",
        createdAt: serverTs,
        updatedAt: serverTs,
      });
      tx.update(userRef, {
        lastOrderId: "order_atomic_1",
        updatedAt: serverTs,
      });
      tx.update(prodRef, {
        stockQuantity: prodSnap.data().stockQuantity - 5,
        salesCount: prodSnap.data().salesCount + 5,
        updatedAt: serverTs,
      });
      tx.update(couponRef, {
        usedCount: couponSnap.data().usedCount + 1,
        updatedAt: serverTs,
      });
    })
  );

  // Cross-user cannot read or list Alice's orders
  await assertFails(bobDb.collection("orders").doc("order_atomic_1").get());
  await assertFails(bobDb.collection("orders").where("userId", "==", ALICE_UID).get());

  // Customer CANNOT alter order totalAmount
  await assertFails(
    aliceDb.collection("orders").doc("order_atomic_1").update({
      totalAmount: 1.0,
      updatedAt: pastDate(),
    })
  );

  // Order manager can advance order status from new_order to preparing
  await assertSucceeds(
    orderMgrDb.collection("orders").doc("order_atomic_1").update({
      orderStatus: "preparing",
      statusNote: "جاري التجهيز بالمخزن",
      updatedAt: pastDate(),
    })
  );

  // Once order is in 'preparing', customer CANNOT cancel it
  await assertFails(
    aliceDb.collection("orders").doc("order_atomic_1").update({
      orderStatus: "cancelled",
      updatedAt: pastDate(),
    })
  );
});

test("Payment Receipts: accountant/admin can verify payment and delete receiptImageUrl anytime; customer cannot delete or tamper with receipt", async () => {
  const ACCOUNTANT_UID = "accountant_1";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("staff").doc(ACCOUNTANT_UID).set({
      uid: ACCOUNTANT_UID,
      fullName: "سارة محاسبة",
      email: "acc@aleman.com",
      role: "accountant",
      permissions: ["manage_payments", "view_reports"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("orders").doc("order_pay_1").set({
      userId: ALICE_UID,
      orderNumber: "EM-PAY01",
      customerName: "أحمد محمد",
      customerEmail: "alice@example.com",
      accountTypeAtOrder: "retail",
      items: [{
        productId: "prod_1",
        productName: "صنف 1",
        sku: "SKU-1",
        unit: "قطعة",
        pricingMode: "retail",
        unitPrice: 100.0,
        quantity: 2,
        lineTotal: 200.0,
      }],
      subtotal: 200.0,
      discountAmount: 0.0,
      deliveryFee: 20.0,
      totalAmount: 220.0,
      paymentMethod: "instapay",
      paymentStatus: "pending_review",
      transferReference: "IP-998877",
      receiptImageUrl: "https://res.cloudinary.com/demo/image/upload/v1/aleman/receipts/r1.webp",
      orderStatus: "new_order",
      deliveryLabel: "المنزل",
      recipientName: "أحمد محمد",
      phone: "01001234567",
      governorate: "القاهرة",
      city: "مدينة نصر",
      streetAddress: "10 شارع النصر",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accountantDb = testEnv.authenticatedContext(ACCOUNTANT_UID).firestore();

  // Customer CANNOT change receiptImageUrl or paymentStatus
  await assertFails(
    aliceDb.collection("orders").doc("order_pay_1").update({
      receiptImageUrl: "https://evil.example/fake.png",
      updatedAt: pastDate(),
    })
  );

  // Accountant can verify payment without altering receipt
  await assertSucceeds(
    accountantDb.collection("orders").doc("order_pay_1").update({
      paymentStatus: "verified",
      statusNote: "تمت مطابقة الإيصال",
      updatedAt: pastDate(),
    })
  );

  // Accountant CANNOT replace receiptImageUrl with a spoofed URL
  await assertFails(
    accountantDb.collection("orders").doc("order_pay_1").update({
      receiptImageUrl: "https://spoof.example/other.png",
      updatedAt: pastDate(),
    })
  );

  // Accountant CAN delete receiptImageUrl at any time to save storage / cleanup (mirrors Kotlin FieldValue.delete())
  const snapAfterVerify = await accountantDb.collection("orders").doc("order_pay_1").get();
  const orderWithoutReceipt = { ...snapAfterVerify.data(), updatedAt: pastDate() };
  delete orderWithoutReceipt.receiptImageUrl;

  // Customer CANNOT remove their own receiptImageUrl
  await assertFails(aliceDb.collection("orders").doc("order_pay_1").set(orderWithoutReceipt));

  // Accountant CAN remove receiptImageUrl
  await assertSucceeds(accountantDb.collection("orders").doc("order_pay_1").set(orderWithoutReceipt));
});

// --- Phase 8 Tests: Banners, Reviews, Notifications, and Store Settings ---

test("Banners & Store Settings: guest reads active banners & store settings only; only manage_banners/settings staff can write", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    const db = ctx.firestore();
    await db.collection("staff").doc(PROD_MGR_UID).set({
      uid: PROD_MGR_UID,
      fullName: "كريم بنرات",
      email: "banners@aleman.com",
      role: "product_manager",
      permissions: ["manage_banners", "manage_store_settings"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("banners").doc("bn_hidden").set({
      title: "بنر مخفي",
      imageUrl: "https://res.cloudinary.com/demo/image/upload/v1/hidden.webp",
      targetType: "none",
      displayOrder: 2,
      isActive: false,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة",
      tagline: "تسوق بذكاء .. عيش أفضل",
      showFeaturedSection: true,
      updatedAt: ts,
    });
    await db.collection("settings").doc("payments").set({
      vodafoneCashNumber: "01000000000",
      codEnabled: true,
      instaPayEnabled: true,
      vodafoneCashEnabled: true,
      updatedAt: ts,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bannerMgrDb = testEnv.authenticatedContext(PROD_MGR_UID).firestore();
  const ts = pastDate();

  // Guest can read settings/store and query active banners, NOT settings/payments or hidden banner
  await assertSucceeds(unauthDb.collection("settings").doc("store").get());
  await assertFails(unauthDb.collection("settings").doc("payments").get());
  await assertSucceeds(unauthDb.collection("banners").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("banners").doc("bn_hidden").get());
  await assertFails(unauthDb.collection("banners").get());

  // Customer cannot create/edit banners or store settings
  await assertFails(
    aliceDb.collection("banners").doc("bn_hack").set({
      title: "بنر مزيف",
      imageUrl: "https://res.cloudinary.com/demo/image/upload/v1/b.webp",
      targetType: "offers",
      displayOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("settings").doc("store").set({ storeName: "اختراق", updatedAt: ts }));

  // Staff with manage_banners & manage_store_settings can manage both
  await assertSucceeds(
    bannerMgrDb.collection("banners").doc("bn_active").set({
      title: "عروض الصيف",
      subtitle: "خصومات حتى 30%",
      imageUrl: "https://res.cloudinary.com/demo/image/upload/v1/summer.webp",
      targetType: "offers",
      displayOrder: 1,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bannerMgrDb.collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة",
      announcementText: "توصيل مجاني للطلبات فوق 1000 ج.م",
      updatedAt: ts,
    })
  );
});

test("Product Reviews: customer creates one review per product; customer_service can hide/delete; cross-user tampering fails", async () => {
  const CS_UID = "cs_1";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("staff").doc(CS_UID).set({
      uid: CS_UID,
      fullName: "نور خدمة عملاء",
      email: "cs@aleman.com",
      role: "customer_service",
      permissions: ["manage_reviews", "manage_customers"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const csDb = testEnv.authenticatedContext(CS_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();
  const reviewId = `prod_oil_${ALICE_UID}`;

  // Customer creates deterministic review for prod_oil
  await assertSucceeds(
    aliceDb.collection("reviews").doc(reviewId).set({
      productId: "prod_oil",
      productName: "زيت طعام 1 لتر",
      userId: ALICE_UID,
      userName: "أحمد محمد",
      rating: 5,
      comment: "منتج ممتاز وتغليف جيد جدًا",
      isHidden: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest can read visible reviews for product, cannot read all unfiltered if hidden exists later
  await assertSucceeds(
    unauthDb.collection("reviews").where("productId", "==", "prod_oil").where("isHidden", "==", false).get()
  );

  // Another user CANNOT edit or delete Alice's review
  await assertFails(bobDb.collection("reviews").doc(reviewId).update({ comment: "تعديل مخرب", updatedAt: pastDate() }));
  await assertFails(bobDb.collection("reviews").doc(reviewId).delete());

  // Customer CANNOT set wrong doc ID or hide own review
  await assertFails(
    aliceDb.collection("reviews").doc("arbitrary_id").set({
      productId: "prod_oil",
      productName: "زيت طعام 1 لتر",
      userId: ALICE_UID,
      userName: "أحمد محمد",
      rating: 4,
      comment: "تعليق",
      isHidden: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("reviews").doc(reviewId).update({ isHidden: true, updatedAt: pastDate() }));

  // Customer service (manage_reviews) CAN hide review, and guest hidden-filtered query excludes it; delete succeeds
  await assertSucceeds(csDb.collection("reviews").doc(reviewId).update({ isHidden: true, updatedAt: pastDate() }));
  await assertFails(unauthDb.collection("reviews").doc(reviewId).get());
  await assertSucceeds(csDb.collection("reviews").doc(reviewId).delete());
});

test("Notifications: customer reads & marks own read; staff can send order/offer alert; cross-user fails", async () => {
  const ORDER_MGR_UID = "order_mgr_notif";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("staff").doc(ORDER_MGR_UID).set({
      uid: ORDER_MGR_UID,
      fullName: "سامي طلبات",
      email: "orders@aleman.com",
      role: "order_manager",
      permissions: ["manage_orders"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const orderMgrDb = testEnv.authenticatedContext(ORDER_MGR_UID).firestore();
  const ts = pastDate();

  // Order manager sends notification to Alice
  await assertSucceeds(
    orderMgrDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "تم شحن طلبك",
      message: "طلبك رقم EM-10001 خرج للتوصيل الآن.",
      type: "order_status",
      referenceId: "order_atomic_1",
      isRead: false,
      createdAt: ts,
    })
  );

  // Alice can read and mark read
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({ isRead: true })
  );

  // Bob CANNOT read or send notifications to Alice
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_hack").set({
      userId: ALICE_UID,
      title: "تنبيه مزيف",
      message: "رسالة",
      type: "general",
      isRead: false,
      createdAt: ts,
    })
  );
});

// --- Phase 10 Tests: Dynamic Owner Email, Flash Sales, GPS Addresses, Delivery Driver Portal ---

test("Dynamic ownerEmail in settings/store grants verified owner access", async () => {
  const NEW_OWNER_UID = "new_owner_1";
  const NEW_OWNER_EMAIL = "new.owner@aleman.store";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة",
      ownerEmail: NEW_OWNER_EMAIL,
      updatedAt: ts,
    });
  });

  const newOwnerDb = testEnv.authenticatedContext(NEW_OWNER_UID, {
    email: NEW_OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(newOwnerDb.collection("staff").get());
  await assertSucceeds(
    newOwnerDb.collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة المحدث",
      ownerEmail: NEW_OWNER_EMAIL,
      updatedAt: pastDate(),
    })
  );
});

test("Flash Sales: active sales readable by customers; only manage_flash_sales/offers staff can create/update/delete", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("staff").doc(PROD_MGR_UID).set({
      uid: PROD_MGR_UID,
      fullName: "مدير عروض",
      email: "promo@aleman.com",
      role: "product_manager",
      permissions: ["manage_flash_sales", "manage_offers"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().collection("flashSales").doc("fs_inactive").set({
      title: "فلاش غير نشط",
      badgeText: "قريبًا",
      startsAt: new Date(Date.now() + 600_000),
      endsAt: new Date(Date.now() + 3_600_000),
      isActive: false,
      items: [{ productId: "prod_oil", flashRetailPrice: 60.0 }],
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const mgrDb = testEnv.authenticatedContext(PROD_MGR_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    mgrDb.collection("flashSales").doc("fs_active").set({
      title: "فلاش المساء",
      badgeText: "5-9 مساءً",
      startsAt: new Date(Date.now() - 600_000),
      endsAt: new Date(Date.now() + 3_600_000),
      isActive: true,
      items: [{ productId: "prod_oil", flashRetailPrice: 55.0, flashWholesalePrice: 49.0, maxQtyPerOrder: 3 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest/customer can list active flash sales, cannot read inactive or write flash sales
  await assertSucceeds(unauthDb.collection("flashSales").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("flashSales").doc("fs_inactive").get());
  await assertFails(
    aliceDb.collection("flashSales").doc("fs_hack").set({
      title: "اختراق",
      badgeText: "",
      startsAt: ts,
      endsAt: new Date(Date.now() + 3_600_000),
      isActive: true,
      items: [{ productId: "prod_oil", flashRetailPrice: 1.0 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cart item with flashSaleId succeeds for own user
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_oil").set({
      userId: ALICE_UID,
      productId: "prod_oil",
      quantity: 2,
      flashSaleId: "fs_active",
      updatedAt: ts,
    })
  );
});

test("GPS Coordinates on Addresses & Delivery Driver assignment and delivery confirmation workflow", async () => {
  const DRIVER_1_UID = "driver_1";
  const DRIVER_2_UID = "driver_2";
  const ORDER_MGR_UID = "order_mgr_drv";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    const db = ctx.firestore();
    await db.collection("staff").doc(ORDER_MGR_UID).set({
      uid: ORDER_MGR_UID,
      fullName: "مسؤول تشغيل",
      email: "ops@aleman.com",
      role: "order_manager",
      permissions: ["manage_orders", "manage_delivery"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("staff").doc(DRIVER_1_UID).set({
      uid: DRIVER_1_UID,
      fullName: "كريم مندوب",
      email: "d1@aleman.com",
      role: "delivery_driver",
      permissions: ["deliver_orders"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("staff").doc(DRIVER_2_UID).set({
      uid: DRIVER_2_UID,
      fullName: "حسن مندوب",
      email: "d2@aleman.com",
      role: "delivery_driver",
      permissions: ["deliver_orders"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("orders").doc("order_drv_1").set({
      userId: ALICE_UID,
      orderNumber: "EM-DRV01",
      customerName: "أحمد محمد",
      customerEmail: "alice@example.com",
      accountTypeAtOrder: "retail",
      items: [{ productId: "prod_1", productName: "مسحوق غسيل", sku: "DET-1", unit: "عبوة", pricingMode: "retail", unitPrice: 150.0, quantity: 2, lineTotal: 300.0 }],
      subtotal: 300.0,
      discountAmount: 0.0,
      deliveryFee: 25.0,
      totalAmount: 325.0,
      paymentMethod: "cod",
      paymentStatus: "pending_review",
      orderStatus: "ready_for_delivery",
      deliveryLabel: "GPS",
      recipientName: "أحمد محمد",
      phone: "01001234567",
      governorate: "GPS",
      city: "موقع الخريطة",
      streetAddress: "إحداثيات الموقع: 30.04442, 31.23571",
      latitude: 30.04442,
      longitude: 31.23571,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderMgrDb = testEnv.authenticatedContext(ORDER_MGR_UID).firestore();
  const driver1Db = testEnv.authenticatedContext(DRIVER_1_UID).firestore();
  const driver2Db = testEnv.authenticatedContext(DRIVER_2_UID).firestore();
  const ts = pastDate();

  // Customer can save GPS coordinates on delivery address
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_gps").set({
      userId: ALICE_UID,
      label: "المنزل",
      recipientName: "أحمد محمد",
      phone: "01001234567",
      governorate: "GPS",
      city: "موقع الخريطة",
      streetAddress: "إحداثيات الموقع: 30.04442, 31.23571",
      latitude: 30.04442,
      longitude: 31.23571,
      isDefault: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Order manager assigns order_drv_1 to driver_1
  await assertSucceeds(
    orderMgrDb.collection("orders").doc("order_drv_1").update({
      assignedDriverId: DRIVER_1_UID,
      assignedDriverName: "كريم مندوب",
      assignedDriverPhone: "01222222222",
      assignedAt: ts,
      orderStatus: "out_for_delivery",
      statusNote: "تم التوجيه إلى المندوب",
      updatedAt: pastDate(),
    })
  );

  // Driver 1 can query their assigned orders and mark order_drv_1 delivered + notify customer
  await assertSucceeds(driver1Db.collection("orders").where("assignedDriverId", "==", DRIVER_1_UID).get());
  await assertSucceeds(
    driver1Db.collection("orders").doc("order_drv_1").update({
      orderStatus: "delivered",
      statusNote: "تم تسليم الطلب للعميل",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    driver1Db.collection("users").doc(ALICE_UID).collection("notifications").doc("drv_notif_1").set({
      userId: ALICE_UID,
      title: "تم تسليم طلبك",
      message: "تم تسليم الطلب EM-DRV01 بنجاح.",
      type: "order_status",
      referenceId: "order_drv_1",
      isRead: false,
      createdAt: pastDate(),
    })
  );

  // Driver 2 CANNOT read or update orders assigned to Driver 1
  await assertFails(driver2Db.collection("orders").doc("order_drv_1").get());
  await assertFails(
    driver2Db.collection("orders").doc("order_drv_1").update({
      orderStatus: "delivered",
      updatedAt: pastDate(),
    })
  );
});

test("Sequential Order Counter: counters/orders enforces ET-1000+ sequence paired with atomic order checkout", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  const buildOrderPayload = (orderNumber) => ({
    userId: ALICE_UID,
    orderNumber,
    customerName: "أحمد محمد",
    customerEmail: "alice@example.com",
    accountTypeAtOrder: "retail",
    items: [{
      productId: "prod_tea",
      productName: "شاي 250 جم",
      sku: "TEA-250",
      unit: "عبوة",
      pricingMode: "retail",
      unitPrice: 60.0,
      quantity: 1,
      lineTotal: 60.0,
    }],
    subtotal: 60.0,
    discountAmount: 0.0,
    deliveryFee: 20.0,
    totalAmount: 80.0,
    paymentMethod: "cod",
    paymentStatus: "pending_review",
    orderStatus: "new_order",
    deliveryLabel: "المنزل",
    recipientName: "أحمد محمد",
    phone: "01001234567",
    governorate: "القاهرة",
    city: "مدينة نصر",
    streetAddress: "10 شارع النصر",
    createdAt: ts,
    updatedAt: ts,
  });

  // Standalone write to counters/orders without an atomic order transaction fails
  await assertFails(
    aliceDb.collection("counters").doc("orders").set({
      nextNumber: 1001,
      updatedAt: ts,
    })
  );

  // Atomic transaction allocating ET-1000 succeeds
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const counterRef = aliceDb.collection("counters").doc("orders");
      const orderRef = aliceDb.collection("orders").doc("order_seq_1000");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      await tx.get(counterRef);
      tx.set(orderRef, buildOrderPayload("ET-1000"));
      tx.set(counterRef, { nextNumber: 1001, updatedAt: ts });
      tx.update(userRef, { lastOrderId: "order_seq_1000", updatedAt: ts });
    })
  );

  // Attempting to skip sequence numbers (e.g. ET-1005 instead of ET-1001) fails
  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      const counterRef = aliceDb.collection("counters").doc("orders");
      const orderRef = aliceDb.collection("orders").doc("order_seq_skip");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      await tx.get(counterRef);
      tx.set(orderRef, buildOrderPayload("ET-1005"));
      tx.update(counterRef, { nextNumber: 1006, updatedAt: ts });
      tx.update(userRef, { lastOrderId: "order_seq_skip", updatedAt: ts });
    })
  );

  // Second atomic transaction allocating ET-1001 succeeds
  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const counterRef = aliceDb.collection("counters").doc("orders");
      const orderRef = aliceDb.collection("orders").doc("order_seq_1001");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      const snap = await tx.get(counterRef);
      const assigned = snap.data().nextNumber;
      tx.set(orderRef, buildOrderPayload(`ET-${assigned}`));
      tx.update(counterRef, { nextNumber: assigned + 1, updatedAt: ts });
      tx.update(userRef, { lastOrderId: "order_seq_1001", updatedAt: ts });
    })
  );
});

test("Store Pickup: customer creates order with fulfillmentType='pickup' and zero deliveryFee; invalid fulfillmentType rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  const buildPickupOrder = (orderNumber, fulfillmentType) => ({
    userId: ALICE_UID,
    orderNumber,
    customerName: "أحمد محمد",
    customerEmail: "alice@example.com",
    accountTypeAtOrder: "retail",
    items: [{
      productId: "prod_tea",
      productName: "شاي 250 جم",
      sku: "TEA-250",
      unit: "عبوة",
      pricingMode: "retail",
      unitPrice: 60.0,
      quantity: 2,
      lineTotal: 120.0,
    }],
    subtotal: 120.0,
    discountAmount: 0.0,
    deliveryFee: 0.0,
    totalAmount: 120.0,
    paymentMethod: "cod",
    paymentStatus: "pending_review",
    orderStatus: "new_order",
    fulfillmentType,
    deliveryLabel: "استلام من المتجر",
    recipientName: "أحمد محمد",
    phone: "01001234567",
    governorate: "المتجر",
    city: "الفرع الرئيسي",
    streetAddress: "فرع مدينة نصر",
    createdAt: ts,
    updatedAt: ts,
  });

  await assertSucceeds(
    aliceDb.runTransaction(async (tx) => {
      const counterRef = aliceDb.collection("counters").doc("orders");
      const orderRef = aliceDb.collection("orders").doc("order_pickup_1");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      await tx.get(counterRef);
      tx.set(orderRef, buildPickupOrder("ET-1000", "pickup"));
      tx.set(counterRef, { nextNumber: 1001, updatedAt: ts });
      tx.update(userRef, { lastOrderId: "order_pickup_1", updatedAt: ts });
    })
  );

  await assertFails(
    aliceDb.runTransaction(async (tx) => {
      const counterRef = aliceDb.collection("counters").doc("orders");
      const orderRef = aliceDb.collection("orders").doc("order_pickup_invalid");
      const userRef = aliceDb.collection("users").doc(ALICE_UID);
      const snap = await tx.get(counterRef);
      const nextNum = snap.data().nextNumber;
      tx.set(orderRef, buildPickupOrder(`ET-${nextNum}`, "drone"));
      tx.update(counterRef, { nextNumber: nextNum + 1, updatedAt: ts });
      tx.update(userRef, { lastOrderId: "order_pickup_invalid", updatedAt: ts });
    })
  );
});

test("Account Self-Deletion: authenticated user CAN delete own profile & subcollections, CANNOT delete another user", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const ts = pastDate();
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      fullName: "أحمد محمد",
      email: "alice@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      fullName: "محمود علي",
      email: "bob@example.com",
      accountType: "retail",
      wholesaleStatus: "none",
      role: "customer",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Cross-user deletion must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());

  // Self-deletion succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("Wishlist & Cart Variants & Cash Verification Logs: owner/staff isolation works cleanly", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  // Alice adds & deletes her wishlist item
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_1").set({
      userId: ALICE_UID,
      productId: "prod_1",
      createdAt: ts,
    })
  );
  // Bob cannot read or write Alice's wishlist
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_2").set({
      userId: ALICE_UID,
      productId: "prod_2",
      createdAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_1").delete());

  // Alice adds cart item with color & size variants
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_1__v1").set({
      userId: ALICE_UID,
      productId: "prod_1",
      quantity: 2,
      selectedColor: "أحمر",
      selectedSize: "XL",
      updatedAt: ts,
    })
  );

  // Owner creates product with availableColors and availableSizes
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_var_1").set({
      name: "تيشيرت قطن",
      description: "خامة ممتازة",
      categoryId: "cat_1",
      sku: "SKU-VAR-1",
      imageUrls: [],
      availableColors: ["أبيض", "أسود"],
      availableSizes: ["M", "L", "XL"],
      retailPrice: 250,
      wholesalePrice: 180,
      wholesaleMinQty: 5,
      unit: "قطعة",
      stockQuantity: 30,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      salesCount: 0,
      averageRating: 0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cash verification log: customer denied, owner allowed
  await assertFails(
    aliceDb.collection("cashVerificationLogs").doc("log_1").set({
      providerName: "فودافون كاش",
      senderAddress: "VF-Cash",
      status: "verified",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    ownerDb.collection("cashVerificationLogs").doc("log_1").set({
      providerName: "فودافون كاش",
      senderAddress: "VF-Cash",
      senderPhone: "01091415953",
      amount: 1000,
      transactionId: "025904460796",
      matchedOrderId: "order_1",
      matchedOrderNumber: "ET-1000",
      status: "verified",
      reason: "تطابق المبلغ ورقم المرسل",
      createdAt: ts,
    })
  );

  // Combined payment verification + order status preparing update
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("orders").doc("order_cash_1").set({
      userId: ALICE_UID,
      orderNumber: "ET-2001",
      customerName: "أليس",
      customerEmail: "alice@example.com",
      accountTypeAtOrder: "retail",
      items: [
        {
          productId: "prod_var_1",
          productName: "تيشيرت قطن",
          quantity: 4,
          unitPrice: 250,
          lineTotal: 1000,
          selectedColor: "أسود",
          selectedSize: "XL",
        },
      ],
      subtotal: 1000,
      discountAmount: 0,
      deliveryFee: 0,
      totalAmount: 1000,
      paymentMethod: "vodafone_cash",
      paymentStatus: "pending_review",
      senderPhone: "01091415953",
      transferReference: "01091415953",
      orderStatus: "new_order",
      fulfillmentType: "delivery",
      deliveryLabel: "المنزل",
      recipientName: "أليس",
      phone: "01091415953",
      governorate: "القاهرة",
      city: "مدينة نصر",
      streetAddress: "شارع مكرم عبيد 15",
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("staff").doc("pay_only_uid").set({
      uid: "pay_only_uid",
      fullName: "محاسب فقط",
      email: "payonly@example.com",
      role: "accountant",
      permissions: ["manage_payments"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const payOnlyDb = testEnv.authenticatedContext("pay_only_uid", {
    email: "payonly@example.com",
    email_verified: true,
  }).firestore();

  // Accountant with only manage_payments cannot jump orderStatus simultaneously
  await assertFails(
    payOnlyDb.collection("orders").doc("order_cash_1").update({
      paymentStatus: "verified",
      orderStatus: "preparing",
      statusNote: "مراجعة فقط",
      updatedAt: ts,
    })
  );

  // Customer cannot verify payment
  await assertFails(
    aliceDb.collection("orders").doc("order_cash_1").update({
      paymentStatus: "verified",
      orderStatus: "preparing",
      statusNote: "خدعة",
      updatedAt: ts,
    })
  );

  // Owner (has manage_payments + manage_orders) can atomically verify payment and move order to preparing
  await assertSucceeds(
    ownerDb.collection("orders").doc("order_cash_1").update({
      paymentStatus: "verified",
      orderStatus: "preparing",
      statusNote: "تم التحقق آلياً عبر الإيمان كاش",
      updatedAt: ts,
    })
  );
});

test("Brand Showcases & Product Visibility Fields: public read for active only, staff write validation", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  // Customer cannot create a brand showcase
  await assertFails(
    aliceDb.collection("brandShowcases").doc("brand_oxi").set({
      title: "أوكسي",
      bannerImageUrl: "https://example.com/oxi-banner.jpg",
      placement: "below_categories",
      displayOrder: 0,
      isActive: true,
      categories: [{ id: "bcat_1", name: "منعم أقمشة", imageUrl: "https://example.com/c1.jpg", displayOrder: 0 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner can create active brand showcase with company categories
  await assertSucceeds(
    ownerDb.collection("brandShowcases").doc("brand_oxi").set({
      title: "أوكسي",
      subtitle: "عروض أوكسي المميزة",
      bannerImageUrl: "https://example.com/oxi-banner.jpg",
      accentColorHex: "#FCE4EC",
      placement: "below_categories",
      displayOrder: 1,
      isActive: true,
      categories: [{ id: "bcat_1", name: "منعم أقمشة", imageUrl: "https://example.com/c1.jpg", displayOrder: 0 }],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner can create inactive showcase; customer cannot read inactive showcase
  await assertSucceeds(
    ownerDb.collection("brandShowcases").doc("brand_hidden").set({
      title: "عرض مخفي",
      bannerImageUrl: "https://example.com/hidden.jpg",
      placement: "top_home",
      displayOrder: 2,
      isActive: false,
      categories: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("brandShowcases").doc("brand_oxi").get());
  await assertFails(aliceDb.collection("brandShowcases").doc("brand_hidden").get());

  // Owner can create an exclusive brand showcase product linked to brand_oxi / bcat_1
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_oxi_1").set({
      name: "أوكسي منعم أقمشة لافندر",
      description: "منعم أقمشة مركز ١ لتر",
      categoryId: "cat_1",
      sku: "OXI-LAV-1L",
      imageUrls: ["https://example.com/oxi1.jpg"],
      retailPrice: 150.95,
      wholesalePrice: 135,
      wholesaleMinQty: 6,
      unit: "١ لتر",
      stockQuantity: 50,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      brandShowcaseId: "brand_oxi",
      brandCategoryId: "bcat_1",
      exclusiveToBrandShowcase: true,
      salesCount: 0,
      averageRating: 0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Stock Alerts & Restock Notifications: owner/staff scoping and stock_alert delivery", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const invStaffDb = testEnv.authenticatedContext("inv_staff_1", {
    email: "inv1@example.com",
    email_verified: true,
  }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("products").doc("prod_oos_1").set({
      name: "برسيل جل ٢ لتر",
      description: "منظف",
      categoryId: "cat_1",
      sku: "PER-2L",
      imageUrls: ["https://example.com/p.jpg"],
      retailPrice: 120,
      wholesalePrice: 100,
      wholesaleMinQty: 4,
      unit: "عبوة",
      stockQuantity: 0,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      salesCount: 0,
      averageRating: 0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("staff").doc("inv_staff_1").set({
      uid: "inv_staff_1",
      fullName: "مدير مخزون",
      email: "inv1@example.com",
      role: "inventory_manager",
      permissions: ["manage_inventory"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Alice subscribes to back-in-stock alert with deterministic ID
  await assertSucceeds(
    aliceDb.collection("stockAlerts").doc(`prod_oos_1_${ALICE_UID}`).set({
      productId: "prod_oos_1",
      productName: "برسيل جل ٢ لتر",
      userId: ALICE_UID,
      createdAt: ts,
    })
  );

  // Wrong document ID or impersonation fails
  await assertFails(
    aliceDb.collection("stockAlerts").doc("wrong_doc_id").set({
      productId: "prod_oos_1",
      productName: "برسيل جل ٢ لتر",
      userId: ALICE_UID,
      createdAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("stockAlerts").doc(`prod_oos_1_${ALICE_UID}`).get()
  );

  // Owner-scoped query works for Alice; un-scoped list fails for Bob
  await assertSucceeds(
    aliceDb.collection("stockAlerts").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("stockAlerts").get());

  // Inventory manager can list stockAlerts, send stock_alert notification, and delete fulfilled alert
  await assertSucceeds(invStaffDb.collection("stockAlerts").get());
  await assertSucceeds(
    invStaffDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_stock_1").set({
      userId: ALICE_UID,
      title: "توفر المنتج من جديد 🔔",
      message: "أصبح «برسيل جل ٢ لتر» متوفرًا الآن في المخزون!",
      type: "stock_alert",
      referenceId: "prod_oos_1",
      isRead: false,
      createdAt: ts,
    })
  );
  await assertSucceeds(
    invStaffDb.collection("stockAlerts").doc(`prod_oos_1_${ALICE_UID}`).delete()
  );
});

test("Delivery Confirmation OTP & Free Shipping Settings: validation and driver enforcement", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const driverDb = testEnv.authenticatedContext("driver_otp_1", {
    email: "driverotp@example.com",
    email_verified: true,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("staff").doc("driver_otp_1").set({
      uid: "driver_otp_1",
      fullName: "مندوب توصيل",
      email: "driverotp@example.com",
      role: "delivery_driver",
      permissions: ["deliver_orders"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("orders").doc("order_otp_legacy").set({
      userId: ALICE_UID,
      orderNumber: "ET-1900",
      customerName: "أليس",
      customerEmail: "alice@example.com",
      accountTypeAtOrder: "retail",
      items: [{ productId: "p1", productName: "منتج", sku: "S1", unit: "عبوة", pricingMode: "retail", unitPrice: 100, quantity: 1, lineTotal: 100 }],
      subtotal: 100,
      discountAmount: 0,
      deliveryFee: 25,
      totalAmount: 125,
      paymentMethod: "cod",
      paymentStatus: "pending_review",
      orderStatus: "out_for_delivery",
      fulfillmentType: "delivery",
      assignedDriverId: "driver_otp_1",
      deliveryLabel: "المنزل",
      recipientName: "أليس",
      phone: "01012345678",
      governorate: "القاهرة",
      city: "مدينة نصر",
      streetAddress: "شارع 1",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Owner saves free shipping & delivery OTP settings; invalid types/negative threshold rejected
  await assertSucceeds(
    ownerDb.collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة",
      freeShippingEnabled: true,
      freeShippingThreshold: 500,
      deliveryOtpEnabled: false,
      updatedAt: ts,
    })
  );
  await assertFails(
    ownerDb.collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة",
      freeShippingEnabled: true,
      freeShippingThreshold: 500,
      deliveryOtpEnabled: "yes",
      updatedAt: ts,
    })
  );
  await assertFails(
    ownerDb.collection("settings").doc("store").set({
      storeName: "الإيمان للتجارة",
      freeShippingEnabled: true,
      freeShippingThreshold: -10,
      deliveryOtpEnabled: true,
      updatedAt: ts,
    })
  );

  // Alice creates a delivery order with 4-digit deliveryOtp and zero free shipping fee
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_otp_1").set({
      userId: ALICE_UID,
      orderNumber: "ET-1901",
      customerName: "أليس",
      customerEmail: "alice@example.com",
      accountTypeAtOrder: "retail",
      items: [{ productId: "p1", productName: "منتج", sku: "S1", unit: "عبوة", pricingMode: "retail", unitPrice: 600, quantity: 1, lineTotal: 600 }],
      subtotal: 600,
      discountAmount: 0,
      deliveryFee: 0,
      totalAmount: 600,
      paymentMethod: "cod",
      paymentStatus: "pending_review",
      orderStatus: "new_order",
      fulfillmentType: "delivery",
      deliveryLabel: "المنزل",
      recipientName: "أليس",
      phone: "01012345678",
      governorate: "القاهرة",
      city: "مدينة نصر",
      streetAddress: "شارع 1",
      deliveryOtp: "4821",
      deliveryOtpVerified: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Assign order to driver
  await assertSucceeds(
    ownerDb.collection("orders").doc("order_otp_1").update({
      assignedDriverId: "driver_otp_1",
      orderStatus: "out_for_delivery",
      updatedAt: ts,
    })
  );

  // Driver cannot mark order with deliveryOtp as delivered without deliveryOtpVerified == true
  await assertFails(
    driverDb.collection("orders").doc("order_otp_1").update({
      orderStatus: "delivered",
      statusNote: "بدون كود",
      updatedAt: ts,
    })
  );
  // Driver cannot modify deliveryOtp
  await assertFails(
    driverDb.collection("orders").doc("order_otp_1").update({
      deliveryOtp: "0000",
      deliveryOtpVerified: true,
      orderStatus: "delivered",
      updatedAt: ts,
    })
  );
  // Driver succeeds when passing deliveryOtpVerified == true
  await assertSucceeds(
    driverDb.collection("orders").doc("order_otp_1").update({
      orderStatus: "delivered",
      deliveryOtpVerified: true,
      statusNote: "تم التحقق من الكود وتسليم الطلب",
      updatedAt: ts,
    })
  );
  // Legacy order without deliveryOtp can still be marked delivered by driver
  await assertSucceeds(
    driverDb.collection("orders").doc("order_otp_legacy").update({
      orderStatus: "delivered",
      statusNote: "طلب قديم",
      updatedAt: ts,
    })
  );
});

test("Search Misses Analytics: customer atomic log/increment and staff-only report read/delete", async () => {
  const ts = pastDate();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const reportStaffDb = testEnv.authenticatedContext("report_staff_1", {
    email: "rep1@example.com",
    email_verified: true,
  }).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("staff").doc("report_staff_1").set({
      uid: "report_staff_1",
      fullName: "محلل تقارير",
      email: "rep1@example.com",
      role: "accountant",
      permissions: ["view_reports"],
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Customer creates a new search miss record with missCount == 1
  await assertSucceeds(
    aliceDb.collection("searchMisses").doc("sm_abc123").set({
      query: "ممسحة بخار",
      normalizedQuery: "ممسحة بخار",
      missCount: 1,
      lastSource: "android",
      lastSearchedAt: ts,
      createdAt: ts,
    })
  );

  // Incrementing by +1 succeeds, jumping count or altering normalizedQuery fails
  await assertSucceeds(
    aliceDb.collection("searchMisses").doc("sm_abc123").update({
      query: "ممسحه بخار",
      missCount: 2,
      lastSource: "web",
      lastSearchedAt: ts,
    })
  );
  await assertFails(
    aliceDb.collection("searchMisses").doc("sm_abc123").update({
      missCount: 10,
      lastSearchedAt: ts,
    })
  );

  // Customer cannot list or delete searchMisses; staff with view_reports can
  await assertFails(aliceDb.collection("searchMisses").get());
  await assertFails(aliceDb.collection("searchMisses").doc("sm_abc123").delete());
  await assertSucceeds(reportStaffDb.collection("searchMisses").get());
  await assertSucceeds(reportStaffDb.collection("searchMisses").doc("sm_abc123").delete());
});

test("Companies Registry, Product Company Link & Optional-Banner Discounted Showcases", async () => {
  const ts = pastDate();
  const guestDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  // Customer cannot create a company
  await assertFails(
    aliceDb.collection("companies").doc("comp_henkel").set({
      name: "هنكل",
      imageUrl: "https://example.com/henkel.png",
      displayOrder: 0,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner creates active and inactive companies
  await assertSucceeds(
    ownerDb.collection("companies").doc("comp_henkel").set({
      name: "هنكل",
      imageUrl: "https://example.com/henkel.png",
      displayOrder: 0,
      isActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    ownerDb.collection("companies").doc("comp_hidden").set({
      name: "شركة مخفية",
      displayOrder: 1,
      isActive: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Guest/customer can read active company, rejected on inactive
  await assertSucceeds(guestDb.collection("companies").doc("comp_henkel").get());
  await assertFails(guestDb.collection("companies").doc("comp_hidden").get());

  // Owner creates product linked to companyId
  await assertSucceeds(
    ownerDb.collection("products").doc("prod_henkel_1").set({
      name: "برسيل جل ٤ لتر",
      description: "منظف ملابس",
      categoryId: "cat_1",
      companyId: "comp_henkel",
      exclusiveToBrandShowcase: true,
      sku: "HEN-PER-4L",
      imageUrls: ["https://example.com/persil.jpg"],
      retailPrice: 300,
      wholesalePrice: 250,
      wholesaleMinQty: 4,
      unit: "جالون",
      stockQuantity: 25,
      lowStockThreshold: 5,
      isFeatured: false,
      isOffer: false,
      isActive: true,
      salesCount: 0,
      averageRating: 0,
      reviewCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Owner creates a bannerless Company Showcase with companyId + discountPercent
  await assertSucceeds(
    ownerDb.collection("brandShowcases").doc("showcase_henkel").set({
      title: "هنكل",
      subtitle: "عروض منتجات هنكل",
      companyId: "comp_henkel",
      companyImageUrl: "https://example.com/henkel.png",
      discountPercent: 15,
      accentColorHex: "#1565C0",
      placement: "below_categories",
      displayOrder: 0,
      isActive: true,
      categories: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid discount (> 95%) is rejected
  await assertFails(
    ownerDb.collection("brandShowcases").doc("showcase_invalid_disc").set({
      title: "خصم خاطئ",
      companyId: "comp_henkel",
      discountPercent: 99,
      placement: "top_home",
      displayOrder: 0,
      isActive: true,
      categories: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(guestDb.collection("brandShowcases").doc("showcase_henkel").get());
});





