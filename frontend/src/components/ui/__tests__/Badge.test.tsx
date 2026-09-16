import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Badge } from '../Badge';

describe('Badge UI component', () => {
  it('renders children text properly', () => {
    render(<Badge variant="success">COMPLETED</Badge>);
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });

  it('applies danger styling for danger variant', () => {
    const { container } = render(<Badge variant="danger">FAILED</Badge>);
    expect(container.firstChild).toHaveClass('text-rose-400');
  });

  it('renders pulse ping indicator when pulse=true', () => {
    const { container } = render(
      <Badge variant="warning" pulse>
        EXECUTING
      </Badge>
    );
    const pingSpan = container.querySelector('.animate-ping');
    expect(pingSpan).toBeInTheDocument();
  });
});
