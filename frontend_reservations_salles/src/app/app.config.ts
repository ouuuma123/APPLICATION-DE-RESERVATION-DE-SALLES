import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideIcons } from '@ng-icons/core';
import {
  lucideAirplay, lucideArrowLeft, lucideArrowRight, lucideArrowUpRight, lucideBell,
  lucideBookmark, lucideBuilding2, lucideCalendarCheck, lucideCalendarDays,
  lucideChartNoAxesCombined, lucideCheck, lucideChevronDown, lucideChevronLeft,
  lucideChevronRight, lucideCircleCheck, lucideCircleHelp, lucideClipboardList,
  lucideClock3, lucideCoffee, lucideDoorOpen, lucideDownload, lucideEllipsis,
  lucideExternalLink, lucideEye, lucideEyeOff, lucideFileText, lucideFilter,
  lucideGrid2x2, lucideHeadphones, lucideHistory, lucideLaptop, lucideLayers,
  lucideLayoutDashboard, lucideLockKeyhole, lucideLogOut, lucideMail, lucideMapPin,
  lucideMenu, lucideMic, lucideMonitor, lucideMoreHorizontal, lucidePhone, lucidePlus,
  lucidePrinter, lucideProjector, lucideSearch, lucideSend, lucideSettings,
  lucideShieldCheck, lucideSlidersHorizontal, lucideSparkles, lucideSquarePen,
  lucideTrash2, lucideUsers, lucideVideo, lucideWifi, lucideX,
} from '@ng-icons/lucide';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideIcons({
      lucideAirplay, lucideArrowLeft, lucideArrowRight, lucideArrowUpRight, lucideBell,
      lucideBookmark, lucideBuilding2, lucideCalendarCheck, lucideCalendarDays,
      lucideChartNoAxesCombined, lucideCheck, lucideChevronDown, lucideChevronLeft,
      lucideChevronRight, lucideCircleCheck, lucideCircleHelp, lucideClipboardList,
      lucideClock3, lucideCoffee, lucideDoorOpen, lucideDownload, lucideEllipsis,
      lucideExternalLink, lucideEye, lucideEyeOff, lucideFileText, lucideFilter,
      lucideGrid2x2, lucideHeadphones, lucideHistory, lucideLaptop, lucideLayers,
      lucideLayoutDashboard, lucideLockKeyhole, lucideLogOut, lucideMail, lucideMapPin,
      lucideMenu, lucideMic, lucideMonitor, lucideMoreHorizontal, lucidePhone, lucidePlus,
      lucidePrinter, lucideProjector, lucideSearch, lucideSend, lucideSettings,
      lucideShieldCheck, lucideSlidersHorizontal, lucideSparkles, lucideSquarePen,
      lucideTrash2, lucideUsers, lucideVideo, lucideWifi, lucideX,
    }),
  ],
};
