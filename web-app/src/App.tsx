import React from 'react';
import { StoreProvider, useStore } from './context/StoreContext';
import {
  BrandedToast,
  MobileBottomNav,
  StoreFooter,
  StoreHeader,
} from './components/layout/StoreChrome';
import { HomePage } from './pages/customer/HomePage';
import { CategoriesPage } from './pages/customer/CategoriesPage';
import { SearchPage } from './pages/customer/SearchPage';
import { ProductDetailsPage } from './pages/customer/ProductDetailsPage';
import { CartCheckoutPage } from './pages/customer/CartCheckoutPage';
import { OrdersPage } from './pages/customer/OrdersPage';
import { AccountPage } from './pages/customer/AccountPage';
import { AboutStorePage } from './pages/customer/AboutStorePage';
import { StorePoliciesPage } from './pages/customer/StorePoliciesPage';
import { NotificationsPage } from './pages/customer/NotificationsPage';
import { WishlistPage } from './pages/customer/WishlistPage';
import { BrandShowcasePage } from './pages/customer/BrandShowcasePage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';

const AppRouter: React.FC = () => {
  const { route, needsProfileSetup } = useStore();

  const renderPage = () => {
    if (needsProfileSetup && route.name !== 'policies' && route.name !== 'about') {
      return <AccountPage />;
    }

    switch (route.name) {
      case 'home':
        return <HomePage />;
      case 'categories':
        return <CategoriesPage initialCategoryId={route.initialCategoryId} />;
      case 'search':
        return (
          <SearchPage
            initialQuery={route.initialQuery}
            initialSort={route.initialSort}
            onlyOffers={route.onlyOffers}
            initialCategoryId={route.initialCategoryId}
          />
        );
      case 'product':
        return <ProductDetailsPage productId={route.productId} flashSaleId={route.flashSaleId} />;
      case 'brandShowcase':
        return <BrandShowcasePage showcaseId={route.showcaseId} />;
      case 'wishlist':
        return <WishlistPage />;
      case 'cart':
        return <CartCheckoutPage />;
      case 'orders':
        return <OrdersPage />;
      case 'account':
        return <AccountPage initialSection="account" />;
      case 'addresses':
        return <AccountPage initialSection="addresses" />;
      case 'about':
        return <AboutStorePage />;
      case 'policies':
        return <StorePoliciesPage initialTab={route.initialTab} />;
      case 'notifications':
        return <NotificationsPage />;
      case 'admin':
        return <AdminDashboardPage initialSection={route.initialSection} />;
      default:
        return <HomePage />;
    }
  };

  return (
    <div dir="rtl" className="min-h-screen flex flex-col bg-[#F9FAFB] text-[#181A20]">
      <StoreHeader />
      <div className="flex-1 pb-16 md:pb-0">{renderPage()}</div>
      <StoreFooter />
      <MobileBottomNav />
      <BrandedToast />
    </div>
  );
};

export default function App() {
  return (
    <StoreProvider>
      <AppRouter />
    </StoreProvider>
  );
}
