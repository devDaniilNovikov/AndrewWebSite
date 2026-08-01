import { render, screen } from '@testing-library/react';
import { axe } from 'jest-axe';
import { describe, expect, it } from 'vitest';
import { metadata } from '../app/layout';
import { PreviewShell } from '../components/PreviewShell';

describe('PreviewShell', () => {
  it('renders only a neutral, visibly marked preview foundation', () => {
    render(<PreviewShell />);

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: 'Сайт готовится к наполнению',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('Демонстрационная версия')).toBeInTheDocument();
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('publishes noindex and nofollow metadata', () => {
    expect(metadata.robots).toMatchObject({ index: false, follow: false });
  });

  it('has no WCAG A or AA accessibility violations', async () => {
    const { container } = render(<PreviewShell />);

    await expect(axe(container)).resolves.toMatchObject({ violations: [] });
  });
});
