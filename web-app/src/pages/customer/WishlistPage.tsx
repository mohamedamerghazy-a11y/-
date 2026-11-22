import React from 'react';
import { ArrowRight, Heart, ShoppingBag } from 'lucide-react';
import { useStore } from '../../context/StoreContext';
import { ProductCard } from '../../components/common/ProductCard';
import { formatNumber } from '../../lib/formatters';

export const WishlistPage: React.FC = () => {
  const { products, wishlistProductIds, navigate, goBack } = useStore();
  const wishlistProducts = products.filter((p) => wishlistProductIds.includes(p.id));

  return (
    <div className="max-w-[1480px] mx-auto px-3 sm:px-5 lg:px-8 py-5 space-y-6">
      <div className="flex items-center justify-between gap-3 border-b border-[#EBECEF] pb-3.5">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={goBack}
            className="w-9 h-9 rounded-xl bg-white border border-[#EBECEF] flex items-center justify-center hover:bg-[#FFF9DB] transition"
            aria-label="رجوع"
          >
            <ArrowRight className="w-4 h-4" />
          </button>
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-[#181A20] flex items-center gap-2">
              <Heart className="w-5 h-5 fill-[#E53935] text-[#E53935]" />
              <span>المفضلة</span>
            </h1>
            <p className="text-xs text-[#717784]">
              {formatNumber(wishlistProducts.length)} منتج محفوظ للعودة السريعة
            </p>
          </div>
        </div>
      </div>

      {wishlistProducts.length === 0 ? (
        <div className="bg-white border border-[#E8EAEE] rounded-3xl p-12 text-center space-y-4">
          <Heart className="w-14 h-14 mx-auto text-[#C4C9D2]" />
          <h2 className="text-lg font-black text-[#181A20]">قائمة المفضلة فارغة حاليًا</h2>
          <p className="text-xs sm:text-sm text-[#717784] max-w-md mx-auto">
            اضغط على أيقونة القلب على أي منتج لحفظه هنا والوصول إليه بسهولة لاحقًا.
          </p>
          <button
            type="button"
            onClick={() => navigate({ name: 'home' })}
            className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl bg-[#FFD000] hover:bg-[#E5B800] text-[#181A20] font-black text-sm transition"
          >
            <ShoppingBag className="w-4 h-4" />
            <span>تصفح المنتجات</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 2xl:grid-cols-6 gap-2.5 sm:gap-3.5 lg:gap-4">
          {wishlistProducts.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              onOpen={() => navigate({ name: 'product', productId: p.id })}
            />
          ))}
        </div>
      )}
    </div>
  );
};
