# React Advanced Frontend Patterns - Gold Sheet

> Track File #21 of 24 - Group 3: React Architecture And Routing
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

## 7. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Prop explosion | hard API | compound components/composition |
| HOC for new simple logic | unnecessary wrappers | custom hooks |
| Mixing controlled/uncontrolled accidentally | React warnings | clear API rules |
| Hidden context dependencies | hard reuse | document component contract |
| Too clever patterns | team confusion | choose simplest expressive API |

---

## 8. Strong Interview Answer

Question:
When would you use compound components, render props, or HOCs?

Strong answer:

```text
I use compound components when a component family needs shared state and flexible
markup, like Tabs or Accordion. Render props are useful when behavior is reusable
but rendering must be caller-defined, though hooks often replace them now. HOCs
are common in older code or library wrappers, but for new code I usually prefer
hooks and composition. The goal is API ergonomics, not pattern usage for its own
sake.
```

---

## 9. Revision Notes

- One-line summary: Advanced patterns are component API design tools.
- Three keywords: composition, control, reuse.
- One interview trap: Hooks did not make all older patterns irrelevant.
- One memory trick: Use the simplest pattern that keeps the component API pleasant.

