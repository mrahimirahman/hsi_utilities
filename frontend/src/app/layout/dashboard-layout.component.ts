import { Component, DestroyRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { AiAssistantComponent } from '../features/assistant/ai-assistant.component';

interface MenuItem {
  label: string;
  shortLabel: string;
  route: string;
  icon: string;
  requiresAdmin?: boolean;
  exact?: boolean;
  group?: string;
}

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, AiAssistantComponent],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.css'
})
export class DashboardLayoutComponent {
  readonly menuItems: MenuItem[] = [
    { label: 'HSI Reports', shortLabel: 'Laporan', route: '/reports', icon: 'query_stats' },
    { label: 'Update Discharge Time', shortLabel: 'Discaj', route: '/discharge-date', icon: 'schedule' },
    { label: 'Update Death Date', shortLabel: 'Kematian', route: '/death-date', icon: 'event_busy' },
    { label: 'Ticket Report', shortLabel: 'Tiket', route: '/ticket-report', icon: 'confirmation_number', requiresAdmin: true },
    { label: 'Audit Log', shortLabel: 'Audit', route: '/reports/audit', icon: 'verified_user', requiresAdmin: true },
    { label: 'Complete Transfer (Out Waiting)', shortLabel: 'Transfer', route: '/fix/complete-transfer', icon: 'move_down', requiresAdmin: true, group: 'Pembetulan HIS' },
    { label: 'Unlock Document', shortLabel: 'Unlock', route: '/fix/unlock-document', icon: 'lock_open', requiresAdmin: true, group: 'Pembetulan HIS' },
    { label: 'Update Birth Time', shortLabel: 'Masa Lahir', route: '/fix/birth-time', icon: 'child_care', requiresAdmin: true, group: 'Pembetulan HIS' },
    { label: 'Document Status (Mark as Error)', shortLabel: 'Status Dok', route: '/fix/document-status', icon: 'rule', requiresAdmin: true, group: 'Pembetulan HIS' }
  ];

  readonly currentDateLabel = this.formatCurrentDate();
  readonly currentHijriLabel = this.formatCurrentHijriDate();
  readonly drawerOpen = signal(false);
  readonly currentSection = signal('');

  constructor(
    public readonly authService: AuthService,
    private readonly router: Router,
    destroyRef: DestroyRef
  ) {
    this.currentSection.set(this.sectionFor(this.router.url));
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(destroyRef)
      )
      .subscribe((event) => {
        this.currentSection.set(this.sectionFor(event.urlAfterRedirects));
        this.drawerOpen.set(false);
      });
  }

  visibleMenuItems(): MenuItem[] {
    const role = this.authService.currentUser()?.role ?? '';
    return this.menuItems.filter((item) => !item.requiresAdmin || role === 'ADMIN');
  }

  // Heading to render above this item: only when its group differs from the previous visible item's.
  groupHeading(items: MenuItem[], index: number): string | null {
    const group = items[index].group;
    return group && group !== items[index - 1]?.group ? group : null;
  }

  isActive(item: MenuItem): boolean {
    const url = this.router.url.split('?')[0];
    if (item.route === '/reports') {
      // Audit Log lives under /reports but has its own menu entry.
      return url.startsWith('/reports') && !url.startsWith('/reports/audit');
    }
    return url === item.route || url.startsWith(item.route + '/');
  }

  canUseAiAssistant(): boolean {
    return this.authService.currentUser()?.role === 'ADMIN';
  }

  initials(): string {
    const name = this.authService.currentUser()?.fullName ?? '';
    const parts = name.split(/\s+/).filter((part) => part && !/^(bin|binti|bt|b\.?)$/i.test(part));
    return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U';
  }

  toggleDrawer(): void {
    this.drawerOpen.update((open) => !open);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      void this.router.navigate(['/login']);
    });
  }

  private sectionFor(url: string): string {
    const path = url.split('?')[0];
    const match = [...this.menuItems]
      .sort((a, b) => b.route.length - a.route.length)
      .find((item) => path === item.route || path.startsWith(item.route + '/'));
    return match?.label ?? '';
  }

  private formatCurrentDate(): string {
    return new Intl.DateTimeFormat('en-GB', {
      weekday: 'long',
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(new Date());
  }

  private formatCurrentHijriDate(): string {
    const label = new Intl.DateTimeFormat('ms-MY-u-ca-islamic', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(new Date());

    return label.replace(/\s*[Hh]$/, '').trim() + ' H';
  }
}
