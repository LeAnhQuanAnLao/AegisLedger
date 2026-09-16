import { describe, it, expect } from 'vitest';
import { formatCurrency, truncateId, generateUUID } from '../formatters';

describe('formatters utility', () => {
  it('formats USD currency properly', () => {
    const result = formatCurrency(125000, 'USD');
    expect(result).toContain('125,000');
  });

  it('formats VND currency properly without decimal digits', () => {
    const result = formatCurrency(5000000, 'VND');
    expect(result).toContain('5.000.000');
  });

  it('truncates UUID properly with ellipsis', () => {
    const uuid = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
    const truncated = truncateId(uuid, 6, 4);
    expect(truncated).toBe('f47ac1...d479');
  });

  it('generates valid UUIDv4 strings', () => {
    const uuid = generateUUID();
    expect(uuid).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
  });
});
