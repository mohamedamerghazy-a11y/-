import React from 'react';
import { Bell, BellRing, CheckCheck, PackageCheck, Trash2 } from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import {
  deleteNotificationDoc,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../services/firestoreService';
import { formatDateTime, formatNumber } from '../../lib/formatters';

export const NotificationsPage: React.FC = () => {
  const { firebaseUser, notifications, unreadNotificationsCount, navigate, showToast } = useStore();

  if (!firebaseUser) {
    return (
      <div className="max-w-lg mx-auto px-4 py-14 text-center">
        <p className="text-sm text-[#717784]">يرجى تسجيل الدخول لعرض الإشعارات.</p>
      </div>
    );
  }

  const enableBrowserNotifications = async () => {
    if (!('Notification' in window)) {
      showToast('متصفحك لا يدعم إشعارات سطح المكتب.', 'info');
      return;
    }
    const perm = await Notification.requestPermission();
    if (perm === 'granted') {
      showToast('تم تفعيل إشعارات المتصفح الفورية مع التنبيه الصوتي', 'success');
    } else {
      showToast('تم رفض إذن إشعارات المتصفح — ستظل تصلك الإشعارات داخل المتجر.', 'info');
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-3 sm:px-6 py-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-black text-[#181A20]">
            الإشعارات ({formatNumber(notifications.length)})
          </h1>
          <p className="text-xs text-[#717784]">
            غير مقروء: {formatNumber(unreadNotificationsCount)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={enableBrowserNotifications}
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-[#FFF9DB] border border-[#FFD000] text-xs font-bold"
          >
            <BellRing className="w-4 h-4" />
            <span>تفعيل إشعارات المتصفح</span>
          </button>
          {unreadNotificationsCount > 0 && (
            <button
              type="button"
              onClick={() =>
                markAllNotificationsRead(
                  firebaseUser.uid,
                  notifications.filter((n) => !n.isRead).map((n) => n.id),
                )
              }
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-[#181A20] text-[#FFD000] text-xs font-bold"
            >
              <CheckCheck className="w-4 h-4" />
              <span>تحديد الكل كمقروء</span>
            </button>
          )}
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="bg-white border border-[#EBECEF] rounded-3xl p-10 text-center space-y-2">
          <Bell className="w-12 h-12 mx-auto text-[#717784]" />
          <p className="text-sm font-bold text-[#717784]">لا توجد إشعارات حاليًا</p>
        </div>
      ) : (
        <div className="space-y-2.5">
          {notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => {
                if (!n.isRead) markNotificationRead(firebaseUser.uid, n.id);
                if (n.type === 'order_status' || n.type === 'payment_status') {
                  navigate({ name: 'orders' });
                } else if (n.type === 'stock_alert' && n.referenceId) {
                  navigate({ name: 'product', productId: n.referenceId });
                }
              }}
              className={`p-4 rounded-2xl border cursor-pointer transition flex items-start justify-between gap-3 ${
                n.isRead
                  ? 'bg-white border-[#EBECEF]'
                  : 'bg-[#FFF9DB]/70 border-[#FFD000]'
              }`}
            >
              <div className="space-y-1">
                <div className="font-black text-sm text-[#181A20] flex items-center gap-2">
                  {!n.isRead && <span className="w-2 h-2 rounded-full bg-[#E53935]" />}
                  {n.type === 'stock_alert' && <PackageCheck className="w-4 h-4 text-[#219653]" />}
                  <span>{n.title}</span>
                </div>
                <p className="text-xs text-[#717784] leading-relaxed">{n.message}</p>
                <div className="text-[11px] text-[#717784]">{formatDateTime(n.createdAt)}</div>
              </div>
              <button
                type="button"
                aria-label="حذف الإشعار"
                onClick={(e) => {
                  e.stopPropagation();
                  deleteNotificationDoc(firebaseUser.uid, n.id);
                }}
                className="text-[#E53935] p-1"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
