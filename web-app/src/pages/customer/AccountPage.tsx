import React, { useEffect, useState } from 'react';
import {
  Bell,
  Building2,
  FileText,
  Heart,
  Info,
  LayoutDashboard,
  LogOut,
  MapPin,
  Navigation,
  Package,
  ShieldAlert,
  ShoppingBag,
  Trash2,
  UserCheck,
} from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import {
  createUserProfileDoc,
  deleteCurrentAccountWithReauth,
  deleteDeliveryAddress,
  saveDeliveryAddress,
  submitWholesaleUpgradeRequest,
  updateUserProfileDetails,
} from '../../services/firestoreService';
import { formatDigits, toArabicErrorMessage } from '../../lib/formatters';

export const AccountPage: React.FC<{ initialSection?: 'addresses' | 'account' }> = ({ initialSection = 'account' }) => {
  const {
    firebaseUser,
    userProfile,
    needsProfileSetup,
    signInWithGoogle,
    signOutUser,
    canAccessAdmin,
    effectiveRoleLabel,
    addresses,
    wishlistProductIds,
    navigate,
    showToast,
  } = useStore();

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [wantWholesale, setWantWholesale] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);

  const [showWholesaleModal, setShowWholesaleModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deletingAccount, setDeletingAccount] = useState(false);

  // Addresses Section State
  const [showAddressesPanel, setShowAddressesPanel] = useState(initialSection === 'addresses');
  const [addrLabel, setAddrLabel] = useState('المنزل');
  const [addrRecipient, setAddrRecipient] = useState('');
  const [addrPhone, setAddrPhone] = useState('');
  const [addrGov, setAddrGov] = useState('');
  const [addrCity, setAddrCity] = useState('');
  const [addrStreet, setAddrStreet] = useState('');
  const [addrLandmark, setAddrLandmark] = useState('');
  const [addrLat, setAddrLat] = useState<number | undefined>();
  const [addrLng, setAddrLng] = useState<number | undefined>();

  useEffect(() => {
    if (initialSection === 'addresses') setShowAddressesPanel(true);
  }, [initialSection]);

  useEffect(() => {
    if (userProfile) {
      setFullName(userProfile.fullName);
      setPhone(userProfile.phone || '');
      setCompanyName(userProfile.wholesaleCompanyName || '');
      setAddrRecipient((v) => v || userProfile.fullName);
      setAddrPhone((v) => v || userProfile.phone || '');
    } else if (firebaseUser) {
      setFullName(firebaseUser.displayName || '');
    }
  }, [userProfile, firebaseUser]);

  // Clean Google Sign-In Screen (without unnecessary explanatory text)
  if (!firebaseUser) {
    return (
      <div className="max-w-md mx-auto px-4 py-14">
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-8 text-center space-y-6 shadow-xs">
          <div className="w-16 h-16 mx-auto rounded-2xl bg-[#181A20] text-[#FFD000] flex items-center justify-center shadow-sm">
            <ShoppingBag className="w-8 h-8" />
          </div>
          <h1 className="text-xl font-black text-[#181A20]">تسجيل الدخول</h1>
          <button
            type="button"
            onClick={signInWithGoogle}
            className="w-full py-3.5 rounded-2xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black text-sm shadow-xs transition"
          >
            المتابعة بحساب Google
          </button>
          <div className="pt-2 border-t border-[#EBECEF] flex flex-wrap justify-center gap-3 text-xs text-[#717784]">
            <button type="button" onClick={() => navigate({ name: 'about' })} className="hover:text-[#181A20] font-bold">
              عن المتجر
            </button>
            <span>•</span>
            <button type="button" onClick={() => navigate({ name: 'policies', initialTab: 'privacy' })} className="hover:text-[#181A20] font-bold">
              الخصوصية والسياسات
            </button>
          </div>
        </div>
      </div>
    );
  }

  // First-time Profile Setup Form
  if (needsProfileSetup) {
    const handleCreate = async (e: React.FormEvent) => {
      e.preventDefault();
      if (fullName.trim().length < 2) {
        showToast('يرجى إدخال الاسم الكامل (حرفين على الأقل).', 'error');
        return;
      }
      setSavingProfile(true);
      try {
        await createUserProfileDoc({
          fullName,
          email: firebaseUser.email || 'user@aleman.store',
          phone,
          photoUrl: firebaseUser.photoURL || undefined,
          requestWholesale: wantWholesale,
          companyName,
        });
        showToast('تم إنشاء ملف حسابك بنجاح!', 'success');
      } catch (err) {
        showToast(toArabicErrorMessage(err, 'تعذر حفظ بيانات الحساب.'), 'error');
      } finally {
        setSavingProfile(false);
      }
    };

    return (
      <div className="max-w-lg mx-auto px-4 py-8">
        <form onSubmit={handleCreate} className="bg-white border border-[#EBECEF] rounded-3xl p-6 space-y-4">
          <h1 className="text-lg font-black text-[#181A20]">استكمال بيانات الحساب</h1>
          <div>
            <label className="block text-xs font-bold mb-1">الاسم الكامل *</label>
            <input
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm"
              required
            />
          </div>
          <div>
            <label className="block text-xs font-bold mb-1">رقم الهاتف</label>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm"
            />
          </div>

          <label className="flex items-center gap-2 p-3 rounded-xl bg-[#FFF9DB] border border-[#FFD000] cursor-pointer text-xs font-bold">
            <input
              type="checkbox"
              checked={wantWholesale}
              onChange={(e) => setWantWholesale(e.target.checked)}
            />
            <span>أرغب في التقديم لحساب تاجر (جملة)</span>
          </label>

          {wantWholesale && (
            <div>
              <label className="block text-xs font-bold mb-1">اسم النشاط التجاري / الشركة</label>
              <input
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm"
              />
            </div>
          )}

          <button
            type="submit"
            disabled={savingProfile}
            className="w-full py-3 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm"
          >
            {savingProfile ? 'جاري الحفظ...' : 'حفظ والمتابعة'}
          </button>
        </form>
      </div>
    );
  }

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingProfile(true);
    try {
      await updateUserProfileDetails({ fullName, phone, companyName });
      showToast('تم تحديث بيانات الحساب بنجاح', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر تحديث البيانات.'), 'error');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleRequestWholesale = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!companyName.trim() || phone.trim().length < 6) {
      showToast('يرجى إدخال اسم المحل أو الشركة ورقم الهاتف.', 'error');
      return;
    }
    try {
      await submitWholesaleUpgradeRequest({ fullName, phone, companyName });
      setShowWholesaleModal(false);
      showToast('تم إرسال طلب تفعيل حساب الجملة للمراجعة.', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر إرسال الطلب.'), 'error');
    }
  };

  const handleDeleteAccount = async () => {
    setDeletingAccount(true);
    try {
      await deleteCurrentAccountWithReauth();
      setShowDeleteConfirm(false);
      showToast('تم حذف حسابك وبياناتك الشخصية نهائيًا.', 'info');
      navigate({ name: 'home' });
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حذف الحساب حاليًا.'), 'error');
    } finally {
      setDeletingAccount(false);
    }
  };

  const captureGps = () => {
    if (!navigator.geolocation) {
      showToast('المتصفح لا يدعم الموقع الجغرافي.', 'error');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setAddrLat(pos.coords.latitude);
        setAddrLng(pos.coords.longitude);
        showToast('تم التقاط الإحداثيات بنجاح', 'success');
      },
      () => showToast('تعذر الوصول للموقع الجغرافي.', 'error'),
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  const handleAddAddr = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await saveDeliveryAddress({
        label: addrLabel || 'العنوان',
        recipientName: addrRecipient,
        phone: addrPhone,
        governorate: addrGov,
        city: addrCity,
        streetAddress: addrStreet,
        landmark: addrLandmark || undefined,
        latitude: addrLat,
        longitude: addrLng,
        isDefault: addresses.length === 0,
      });
      setAddrStreet('');
      setAddrLandmark('');
      showToast('تمت إضافة العنوان بنجاح', 'success');
    } catch (err) {
      showToast(toArabicErrorMessage(err, 'تعذر حفظ العنوان.'), 'error');
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-3 sm:px-6 py-5 space-y-5">
      {/* Header Card */}
      <div className="bg-[#181A20] text-white rounded-3xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {userProfile?.photoUrl || firebaseUser.photoURL ? (
            <img
              src={userProfile?.photoUrl || firebaseUser.photoURL || ''}
              alt=""
              className="w-14 h-14 rounded-2xl object-cover border-2 border-[#FFD000]"
            />
          ) : (
            <div className="w-14 h-14 rounded-2xl bg-[#FFD000] text-[#181A20] flex items-center justify-center font-black text-xl">
              {(userProfile?.fullName || 'ع')[0]}
            </div>
          )}
          <div>
            <h1 className="font-black text-lg text-[#FFD000]">{userProfile?.fullName || firebaseUser.displayName}</h1>
            <p className="text-xs text-white/75" dir="ltr">{firebaseUser.email}</p>
            <div className="flex flex-wrap gap-1.5 mt-1">
              {canAccessAdmin && (
                <span className="px-2.5 py-0.5 rounded-full bg-[#FFD000] text-[#181A20] text-[10px] font-black">
                  {effectiveRoleLabel}
                </span>
              )}
              {userProfile?.wholesaleStatus === 'approved' && (
                <span className="px-2.5 py-0.5 rounded-full bg-[#219653] text-white text-[10px] font-black">
                  حساب جملة معتمد
                </span>
              )}
              {userProfile?.wholesaleStatus === 'pending' && (
                <span className="px-2.5 py-0.5 rounded-full bg-amber-500 text-white text-[10px] font-black">
                  طلب الجملة قيد المراجعة
                </span>
              )}
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={signOutUser}
          className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-bold"
        >
          <LogOut className="w-4 h-4" />
          <span>تسجيل الخروج</span>
        </button>
      </div>

      {/* Quick Navigation Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {canAccessAdmin && (
          <button
            type="button"
            onClick={() => navigate({ name: 'admin' })}
            className="p-4 rounded-2xl bg-[#FFF9DB] border border-[#FFD000] text-right space-y-1 hover:shadow-sm"
          >
            <LayoutDashboard className="w-5 h-5 text-[#181A20]" />
            <div className="font-black text-sm">لوحة الإدارة</div>
            <div className="text-[11px] text-[#717784]">المنتجات والطلبات</div>
          </button>
        )}

        <button
          type="button"
          onClick={() => navigate({ name: 'orders' })}
          className="p-4 rounded-2xl bg-white border border-[#EBECEF] text-right space-y-1 hover:border-[#FFD000]"
        >
          <Package className="w-5 h-5 text-[#181A20]" />
          <div className="font-black text-sm">طلباتي</div>
          <div className="text-[11px] text-[#717784]">متابعة الفواتير</div>
        </button>

        <button
          type="button"
          onClick={() => navigate({ name: 'wishlist' })}
          className="p-4 rounded-2xl bg-white border border-[#EBECEF] text-right space-y-1 hover:border-[#FFD000]"
        >
          <Heart className="w-5 h-5 text-[#E53935]" />
          <div className="font-black text-sm">المفضلة</div>
          <div className="text-[11px] text-[#717784]">{formatDigits(wishlistProductIds.length)} منتج محفوظ</div>
        </button>

        <button
          type="button"
          onClick={() => setShowAddressesPanel((v) => !v)}
          className="p-4 rounded-2xl bg-white border border-[#EBECEF] text-right space-y-1 hover:border-[#FFD000]"
        >
          <MapPin className="w-5 h-5 text-[#1565C0]" />
          <div className="font-black text-sm">عناوين التوصيل</div>
          <div className="text-[11px] text-[#717784]">{formatDigits(addresses.length)} عنوان محفوظ</div>
        </button>

        <button
          type="button"
          onClick={() => navigate({ name: 'notifications' })}
          className="p-4 rounded-2xl bg-white border border-[#EBECEF] text-right space-y-1 hover:border-[#FFD000]"
        >
          <Bell className="w-5 h-5 text-[#181A20]" />
          <div className="font-black text-sm">الإشعارات</div>
          <div className="text-[11px] text-[#717784]">التنبيهات والعروض</div>
        </button>

        <button
          type="button"
          onClick={() => navigate({ name: 'about' })}
          className="p-4 rounded-2xl bg-white border border-[#EBECEF] text-right space-y-1 hover:border-[#FFD000]"
        >
          <Info className="w-5 h-5 text-[#181A20]" />
          <div className="font-black text-sm">عن المتجر</div>
          <div className="text-[11px] text-[#717784]">التواصل وصفحاتنا</div>
        </button>

        <button
          type="button"
          onClick={() => navigate({ name: 'policies', initialTab: 'privacy' })}
          className="p-4 rounded-2xl bg-white border border-[#EBECEF] text-right space-y-1 hover:border-[#FFD000]"
        >
          <FileText className="w-5 h-5 text-[#181A20]" />
          <div className="font-black text-sm">السياسات والخصوصية</div>
          <div className="text-[11px] text-[#717784]">الشروط والاسترجاع</div>
        </button>
      </div>

      {/* Wholesale Request Card */}
      {userProfile && (userProfile.wholesaleStatus === 'none' || userProfile.wholesaleStatus === 'rejected') && (
        <div className="bg-[#FFF9DB] border border-[#FFD000] rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Building2 className="w-6 h-6 text-[#181A20]" />
            <div>
              <div className="font-black text-sm">هل أنت تاجر أو موزع؟</div>
              <p className="text-xs text-[#717784]">اطلب تفعيل حساب الجملة للاستفادة من أسعار الكميات المخصصة للتجار.</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setShowWholesaleModal(true)}
            className="px-4 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-black"
          >
            طلب حساب جملة
          </button>
        </div>
      )}

      {/* Addresses Panel */}
      {showAddressesPanel && (
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-5 space-y-4">
          <h2 className="font-black text-base">عناوين التوصيل المحفوظة</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {addresses.map((addr) => (
              <div key={addr.id} className="p-3.5 rounded-2xl bg-[#F9FAFB] border border-[#EBECEF] flex items-start justify-between gap-2 text-xs">
                <div>
                  <div className="font-black text-[#181A20]">
                    {addr.label} {addr.isDefault ? '• افتراضي' : ''}
                  </div>
                  <div className="text-[#717784] mt-0.5">
                    {addr.recipientName} — {formatDigits(addr.phone)}
                  </div>
                  <div className="mt-1">
                    {addr.governorate} - {addr.city} - {addr.streetAddress}
                  </div>
                  {addr.latitude != null && addr.longitude != null && (
                    <div className="text-[11px] text-[#219653] font-bold mt-1">
                      GPS: {addr.latitude.toFixed(4)}, {addr.longitude.toFixed(4)}
                    </div>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => deleteDeliveryAddress(addr.id)}
                  className="text-[#E53935] p-1"
                  title="حذف العنوان"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>

          <form onSubmit={handleAddAddr} className="pt-3 border-t border-[#EBECEF] space-y-3">
            <div className="text-xs font-black">إضافة عنوان جديد</div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs">
              <input value={addrLabel} onChange={(e) => setAddrLabel(e.target.value)} placeholder="التسمية (المنزل/المخزن)" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input value={addrRecipient} onChange={(e) => setAddrRecipient(e.target.value)} placeholder="اسم المستلم" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input value={addrPhone} onChange={(e) => setAddrPhone(e.target.value)} placeholder="رقم الهاتف" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input value={addrGov} onChange={(e) => setAddrGov(e.target.value)} placeholder="المحافظة" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input value={addrCity} onChange={(e) => setAddrCity(e.target.value)} placeholder="المدينة" className="h-10 px-3 rounded-xl border border-[#EBECEF]" required />
              <input value={addrLandmark} onChange={(e) => setAddrLandmark(e.target.value)} placeholder="علامة مميزة" className="h-10 px-3 rounded-xl border border-[#EBECEF]" />
            </div>
            <input value={addrStreet} onChange={(e) => setAddrStreet(e.target.value)} placeholder="الشارع والعقار بالتفصيل" className="w-full h-10 px-3 rounded-xl border border-[#EBECEF] text-xs" required />
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={captureGps}
                className="px-4 py-2 rounded-xl bg-[#EBF4FF] text-[#1565C0] text-xs font-bold inline-flex items-center gap-1.5"
              >
                <Navigation className="w-3.5 h-3.5" />
                {addrLat != null ? `تم الالتقاط (${addrLat.toFixed(3)}, ${addrLng?.toFixed(3)})` : 'التقاط إحداثيات GPS'}
              </button>
              <button type="submit" className="px-5 py-2 rounded-xl bg-[#FFD000] text-[#181A20] text-xs font-black">
                حفظ العنوان
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Profile Editor */}
      <form onSubmit={handleSaveProfile} className="bg-white border border-[#EBECEF] rounded-3xl p-5 space-y-4">
        <div className="flex items-center gap-2 font-black text-base">
          <UserCheck className="w-5 h-5" />
          <span>بيانات الحساب الشخصية</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <div>
            <label className="block text-xs font-bold text-[#717784] mb-1">الاسم الكامل</label>
            <input
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
              required
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-[#717784] mb-1">رقم الهاتف</label>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full h-10 px-3 rounded-xl border border-[#EBECEF]"
            />
          </div>
        </div>
        <button
          type="submit"
          disabled={savingProfile}
          className="px-6 py-2.5 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-black"
        >
          {savingProfile ? 'جاري الحفظ...' : 'حفظ التغييرات'}
        </button>
      </form>

      {/* Permanent Account Deletion Zone */}
      <div className="bg-[#FDECEC]/60 border border-[#E53935]/30 rounded-3xl p-5 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <ShieldAlert className="w-6 h-6 text-[#E53935]" />
          <div>
            <div className="font-black text-sm text-[#E53935]">حذف الحساب نهائيًا</div>
            <p className="text-xs text-[#717784]">يتم مسح بيانات حسابك وعناوينك وسلة التسوق وإشعاراتك وتقييماتك فورًا.</p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setShowDeleteConfirm(true)}
          className="px-4 py-2 rounded-xl bg-[#E53935] text-white text-xs font-black hover:bg-red-700"
        >
          حذف الحساب نهائيًا
        </button>
      </div>

      {/* Wholesale Modal */}
      {showWholesaleModal && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <form onSubmit={handleRequestWholesale} className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4">
            <h3 className="font-black text-base">تقديم طلب حساب جملة</h3>
            <input
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              placeholder="اسم النشاط التجاري / السوبر ماركت / الشركة"
              className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm"
              required
            />
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="رقم الهاتف للتواصل"
              className="w-full h-11 px-3 rounded-xl border border-[#EBECEF] text-sm"
              required
            />
            <div className="flex gap-2">
              <button type="submit" className="flex-1 py-2.5 rounded-xl bg-[#FFD000] text-[#181A20] font-black text-sm">
                إرسال الطلب
              </button>
              <button type="button" onClick={() => setShowWholesaleModal(false)} className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold">
                إلغاء
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete Account Confirm Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 bg-black/55 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 border border-[#E53935]/40">
            <h3 className="font-black text-base text-[#E53935]">تأكيد الحذف النهائي للحساب</h3>
            <p className="text-xs text-[#717784] leading-relaxed">
              هذا الإجراء نهائي ولا يمكن التراجع عنه. سيتم حذف ملفك الشخصي، عناوين التوصيل، سلة التسوق، إشعاراتك، وتقييماتك فورًا.
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={deletingAccount}
                onClick={handleDeleteAccount}
                className="flex-1 py-2.5 rounded-xl bg-[#E53935] text-white font-black text-xs disabled:opacity-60"
              >
                {deletingAccount ? 'جاري الحذف...' : 'نعم، احذف حسابي نهائيًا'}
              </button>
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(false)}
                className="px-4 py-2.5 rounded-xl border border-[#EBECEF] text-xs font-bold"
              >
                تراجع
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
