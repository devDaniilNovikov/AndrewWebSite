import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

vi.mock('next/font/local', () => ({
  default: () => ({
    className: 'inter-variable-test-font',
    style: { fontFamily: 'InterVariable' },
    variable: 'inter-variable-test-font-variable',
  }),
}));

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  value: (query: string) => ({
    addEventListener() {},
    dispatchEvent: () => false,
    matches: false,
    media: query,
    onchange: null,
    removeEventListener() {},
  }),
});

class IntersectionObserverStub implements IntersectionObserver {
  readonly root = null;
  readonly rootMargin = '0px';
  readonly thresholds = [0];

  disconnect() {}

  observe() {}

  takeRecords() {
    return [];
  }

  unobserve() {}
}

Object.defineProperty(globalThis, 'IntersectionObserver', {
  configurable: true,
  value: IntersectionObserverStub,
});

Object.defineProperties(HTMLDialogElement.prototype, {
  close: {
    configurable: true,
    value(this: HTMLDialogElement) {
      this.removeAttribute('open');
      this.dispatchEvent(new Event('close'));
    },
  },
  showModal: {
    configurable: true,
    value(this: HTMLDialogElement) {
      this.setAttribute('open', '');
    },
  },
});
