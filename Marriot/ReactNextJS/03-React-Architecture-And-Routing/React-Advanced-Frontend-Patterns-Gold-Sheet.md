# React Advanced Frontend Patterns - Gold Sheet

> Track Module - Group 3: React Architecture And Routing
> Covers: compound components, render props, higher order components, controlled vs uncontrolled patterns

---

## 1. Intuition

Advanced component patterns are API design tools.

They help build reusable components without making every use case a prop explosion.

---

## 2. Compound Components

Compound components share implicit state through context.

```tsx
function Tabs({children}: {children: React.ReactNode}) {
  const [value, setValue] = useState('overview');
  return (
    <TabsContext.Provider value={{value, setValue}}>
      {children}
    </TabsContext.Provider>
  );
}

Tabs.List = TabsList;
Tabs.Trigger = TabsTrigger;
Tabs.Panel = TabsPanel;
```

Use for:
- tabs
- accordion
- menu
- select
- modal

Trade-off:
Nice ergonomics, but implementation complexity and context coupling.

---

## 3. Render Props

Render prop passes rendering control to caller.

```tsx
<MouseTracker>
  {position => <Tooltip x={position.x} y={position.y} />}
</MouseTracker>
```

Useful when:
- behavior reusable
- UI shape varies greatly

Modern React often replaces render props with custom hooks, but render props still appear in older libraries and flexible components.

---

## 4. Higher Order Components

HOC wraps a component and returns an enhanced component.

```tsx
function withAuth<P>(Component: React.ComponentType<P>) {
  return function Protected(props: P) {
    const session = useSession();
    if (!session) return <LoginPrompt />;
    return <Component {...props} />;
  };
}
```

Useful for:
- legacy code
- cross-cutting wrappers
- library APIs

Trade-offs:
- wrapper hell
- prop collision
- harder debugging

Hooks often replace HOCs for new code.

---

## 5. Controlled vs Uncontrolled Component APIs

Reusable component can support both:

```tsx
type ToggleProps = {
  checked?: boolean;
  defaultChecked?: boolean;
  onCheckedChange?: (checked: boolean) => void;
};
```

Controlled:
- parent owns value

Uncontrolled:
- component owns initial state

This pattern is common in design-system primitives.

---

## 6. Real-World Use Cases

- Design-system Tabs: compound components.
- Table library: render props for custom cells.
- Auth wrapper in legacy app: HOC.
- Dialog primitive: controlled or uncontrolled open state.
- Form field: render prop or hook for field state.

---

## 7. forwardRef and useImperativeHandle

`forwardRef` lets a parent get a ref to a child DOM node or handle.

```tsx
const FancyInput = React.forwardRef<HTMLInputElement, InputProps>(
  ({label, ...props}, ref) => {
    return (
      <label>
        {label}
        <input ref={ref} {...props} />
      </label>
    );
  }
);

// Parent can focus programmatically
const inputRef = useRef<HTMLInputElement>(null);
<FancyInput ref={inputRef} label="Search" />
inputRef.current?.focus();
```

`useImperativeHandle` exposes only specific methods, not the full DOM node:

```tsx
type DialogHandle = {
  open: () => void;
  close: () => void;
};

const Dialog = React.forwardRef<DialogHandle, DialogProps>(
  ({children}, ref) => {
    const [isOpen, setIsOpen] = useState(false);

    useImperativeHandle(ref, () => ({
      open: () => setIsOpen(true),
      close: () => setIsOpen(false),
    }));

    if (!isOpen) return null;
    return <div role="dialog">{children}</div>;
  }
);

// Parent
const dialogRef = useRef<DialogHandle>(null);
<Button onClick={() => dialogRef.current?.open()}>Open</Button>
<Dialog ref={dialogRef}>Content</Dialog>
```

When to use: design system primitives, animation refs, form focus management.

React 19 note: `forwardRef` wrapper is no longer needed in React 19 — `ref` becomes a regular prop.

---

## 8. Polymorphic Components — The `as` Prop

Polymorphic components render as different HTML elements or components while keeping type safety.

```tsx
type PolymorphicProps<T extends React.ElementType> = {
  as?: T;
  children?: React.ReactNode;
} & Omit<React.ComponentPropsWithoutRef<T>, 'as'>;

function Text<T extends React.ElementType = 'p'>({
  as,
  children,
  ...props
}: PolymorphicProps<T>) {
  const Component = as ?? 'p';
  return <Component {...props}>{children}</Component>;
}

// Usage
<Text>Default paragraph</Text>
<Text as="h1" className="heading">Heading</Text>
<Text as={Link} href="/about">Navigation link</Text>
```

Use in design systems for typography, button, container, and card primitives where the semantic element varies by context.

---

## 9. Slot / asChild Pattern

The `asChild` pattern (popularized by Radix UI) passes rendering to the child component instead of rendering a wrapper element.

Without `asChild`:
```tsx
<Button>Click</Button> // renders <button>Click</button>
```

With `asChild`:
```tsx
<Button asChild>
  <Link href="/checkout">Go to checkout</Link>
</Button>
// renders <a href="/checkout" class="button-styles">Go to checkout</a>
```

Implementation using the Radix `Slot` primitive:

```tsx
import {Slot} from '@radix-ui/react-slot';

function Button({asChild, children, className, ...props}: ButtonProps) {
  const Component = asChild ? Slot : 'button';
  return (
    <Component className={cn('button', className)} {...props}>
      {children}
    </Component>
  );
}
```

Why this matters:
- Composes styles without creating extra DOM nodes
- Avoids invalid HTML nesting (`button > a`)
- Common in Radix UI, shadcn/ui, and design systems built on primitives

---

## 10. Context Factory Pattern

A context factory creates typed context + provider + hook together, preventing the most common context mistakes.

```tsx
function createContext<T>(displayName: string) {
  const Ctx = React.createContext<T | undefined>(undefined);
  Ctx.displayName = displayName;

  function useCtx(): T {
    const value = useContext(Ctx);
    if (value === undefined) {
      throw new Error(`use${displayName} must be inside ${displayName}Provider`);
    }
    return value;
  }

  return [Ctx.Provider, useCtx] as const;
}

// Usage
type ThemeCtx = {isDark: boolean; toggle: () => void};

const [ThemeProvider, useTheme] = createContext<ThemeCtx>('Theme');

function ThemeRoot({children}: {children: React.ReactNode}) {
  const [isDark, setIsDark] = useState(false);
  return (
    <ThemeProvider value={{isDark, toggle: () => setIsDark(p => !p)}}>
      {children}
    </ThemeProvider>
  );
}

// Consumer — throws descriptively if used outside provider
function DarkModeToggle() {
  const {isDark, toggle} = useTheme();
  return <button onClick={toggle}>{isDark ? 'Light' : 'Dark'}</button>;
}
```

Benefits: type safety, clear error messages, removes boilerplate `undefined` checks.

---

## 11. Portal Pattern

Portals render children into a DOM node outside the React tree — needed for modals, tooltips, and dropdown menus that should visually escape overflow/stacking context constraints.

```tsx
import {createPortal} from 'react-dom';

function Modal({isOpen, onClose, children}: ModalProps) {
  if (!isOpen) return null;

  return createPortal(
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-content"
        role="dialog"
        aria-modal="true"
        onClick={e => e.stopPropagation()}
      >
        {children}
      </div>
    </div>,
    document.body // renders outside parent overflow:hidden
  );
}
```

Production considerations:
- Focus trap required for accessibility (use Radix Dialog or focus-trap-react)
- `aria-modal="true"` and `role="dialog"` are required
- Keyboard: `Escape` should close the modal
- Screen readers need focus moved into the portal on open

---

## 12. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Prop explosion | hard API | compound components/composition |
| HOC for new simple logic | unnecessary wrappers | custom hooks |
| Mixing controlled/uncontrolled accidentally | React warnings | clear API rules |
| Hidden context dependencies | hard reuse | document component contract |
| Too clever patterns | team confusion | choose simplest expressive API |
| `forwardRef` without `useImperativeHandle` | exposes full DOM | expose only needed handle |
| Polymorphic without TypeScript constraint | prop type errors at runtime | constrain T extends ElementType |
| `asChild` on non-Radix primitives | unexpected behavior | use Radix Slot |
| Context without factory | undefined crash | use factory with guard |

---

## 13. Strong Interview Answer

Question:
When would you use compound components, render props, HOCs, forwardRef, polymorphic components, or the asChild pattern?

Strong answer:

```text
Compound components are for families like Tabs or Accordion that need shared state
but flexible markup. Render props are for reusable behavior with caller-controlled
rendering, though custom hooks often replace them. HOCs are useful for cross-cutting
concerns in older code. forwardRef exposes a component ref to a parent — in design
systems I combine it with useImperativeHandle to expose only a typed handle, not
the raw DOM node. Polymorphic components with an 'as' prop let primitives like
Text render as different HTML elements while preserving type safety. The asChild/
Slot pattern avoids invalid nesting — for example, a styled Button that renders
as a Link. I use a context factory pattern to co-locate context, provider, and hook,
so consumers get a clear error message if they are outside the provider.
```

---

## 14. Revision Notes

- One-line summary: Advanced patterns are component API design tools for ergonomics and reuse.
- Three keywords: composition, forwardRef, asChild.
- One interview trap: Hooks did not make all older patterns irrelevant — `forwardRef`, compound components, and the `asChild` pattern are still widely used in production design systems.
- One memory trick: Use the simplest pattern that keeps the component API pleasant and prevents DOM/accessibility violations.

