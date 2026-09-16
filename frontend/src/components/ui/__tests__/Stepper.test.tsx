import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Stepper, StepItem } from '../Stepper';

describe('Stepper UI component', () => {
  const mockSteps: StepItem[] = [
    { id: 1, label: 'Hold Funds', subTitle: 'Locked', state: 'completed' },
    { id: 2, label: 'Fraud Check', subTitle: 'Evaluating', state: 'running' },
    { id: 3, label: 'Commit Ledger', subTitle: 'Double Entry', state: 'pending' },
  ];

  it('renders all step labels and subtitles', () => {
    render(<Stepper steps={mockSteps} />);
    expect(screen.getByText('Hold Funds')).toBeInTheDocument();
    expect(screen.getByText('Fraud Check')).toBeInTheDocument();
    expect(screen.getByText('Commit Ledger')).toBeInTheDocument();
    expect(screen.getByText('Double Entry')).toBeInTheDocument();
  });
});
