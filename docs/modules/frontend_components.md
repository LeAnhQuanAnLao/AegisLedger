# Đặc Tả Module: Frontend UI Components & Design System (Tier 1)

> Phiên bản: 1.0.0  
> Trạng thái: Approved  
> Phụ trách: Design System & UI Team

---

## 1. Mục Đích & Phạm Vi (Responsibility Scope)
Cung cấp tập hợp các thành phần giao diện nguyên tử (Atomic UI Components) chuẩn hoá cho hệ thống AegisLedger:
- **Nguyên tắc thiết kế**: Fintech Dark Slate (`#0B0F19`, `#111827`, `#1F2937`), các điểm nhấn Neon Emerald cho giao dịch có/thành công, Electric Indigo cho tiến trình Saga, Cyber Amber cho cảnh báo, Crimson cho lỗi/gian lận.
- **Quy tắc kích thước file**: Mỗi component TUYỆT ĐỐI không vượt quá 150 dòng code.
- **Khả năng tương thích**: Hỗ trợ đầy đủ TypeScript props, Accessible ARIA attributes, Tailwind typography cho dữ liệu số tài chính.

---

## 2. Danh Sách UI Components Chuẩn

### 1. `Button.tsx`
- Props: `variant` ('primary' | 'secondary' | 'danger' | 'ghost' | 'outline'), `size` ('sm' | 'md' | 'lg'), `isLoading?: boolean`, `icon?: ReactNode`.
- Hiệu ứng: Scale nhẹ khi click, spinner khi loading, disabled state mờ đục.

### 2. `Badge.tsx`
- Props: `variant` ('success' | 'warning' | 'danger' | 'info' | 'neutral'), `pulse?: boolean`.
- Ứng dụng: Gắn nhãn trạng thái giao dịch (`COMPLETED`, `PENDING`, `COMPENSATED`), nhãn loại bút toán (`DEBIT`, `CREDIT`), trạng thái tài khoản.

### 3. `Card.tsx`
- Props: `children`, `className`, `header?: ReactNode`, `footer?: ReactNode`, `glow?: boolean`.
- Kiểu dáng: Glassmorphism vi tế, viền `border-slate-800/80`, nền `bg-slate-900/60 backdrop-blur-md`.

### 4. `StatCard.tsx`
- Props: `title: string`, `value: string | number`, `change?: string`, `isPositive?: boolean`, `icon: LucideIcon`, `subtitle?: string`.
- Ứng dụng: Hiển thị 4 chỉ số KPI lớn ở đầu Dashboard.

### 5. `Input.tsx` & `Select.tsx`
- Props: `label`, `error?: string`, `helperText?: string`, `leftAddon?: ReactNode`, `rightAddon?: ReactNode`.
- Ứng dụng: Nhập số tiền, mã Idempotency, tên chủ tài khoản, chọn tiền tệ.

### 6. `Modal.tsx`
- Props: `isOpen: boolean`, `onClose: () => void`, `title: string`, `children: ReactNode`.
- Ứng dụng: Modal tạo tài khoản mới, modal chi tiết giao dịch, modal xác nhận chuyển khoản.

### 7. `Stepper.tsx`
- Props: `steps: { label: string; description?: string; status: 'completed' | 'current' | 'upcoming' | 'failed' }[]`.
- Ứng dụng: Hiển thị trực quan 5 bước của Saga Pattern.

### 8. `Table.tsx`
- Component bảng phân trang dữ liệu tài chính với header cố định, hover dòng, empty state trang nhã.

---

## 3. Tiêu Chí Kiểm Thử (Testing Criteria)
- [ ] Unit test: `Badge` render đúng class màu theo variant và render pulse dot khi pulse=true.
- [ ] Unit test: `Button` vô hiệu hoá khi isLoading=true hoặc disabled=true.
