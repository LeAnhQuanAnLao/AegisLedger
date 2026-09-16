import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SagaVisualizer } from '../SagaVisualizer';

describe('SagaVisualizer component', () => {
  it('renders compensation alert when transaction status is COMPENSATED', () => {
    render(
      <SagaVisualizer
        currentStep="COMPENSATED"
        status="COMPENSATED"
        failureReason="Simulated partner timeout"
      />
    );
    expect(screen.getByText('Saga Automatic Compensation Triggered')).toBeInTheDocument();
    expect(screen.getByText('Simulated partner timeout')).toBeInTheDocument();
  });

  it('renders completed banner when transaction status is COMPLETED', () => {
    render(
      <SagaVisualizer
        currentStep="COMMITTED"
        status="COMPLETED"
      />
    );
    expect(
      screen.getByText(/Atomic Saga Completed: Both ledger entries/i)
    ).toBeInTheDocument();
  });
});
