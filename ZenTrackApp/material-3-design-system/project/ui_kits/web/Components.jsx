/* global React */
const { useState } = React;

// --- Button ---
function Button({ variant = "filled", icon, children, onClick, style }) {
  const base = {
    display: "inline-flex", alignItems: "center", gap: 8,
    height: 40, padding: "0 24px", border: "none", cursor: "pointer",
    borderRadius: "var(--md-sys-shape-corner-full)",
    font: "var(--md-sys-typescale-label-large-weight) var(--md-sys-typescale-label-large-size)/var(--md-sys-typescale-label-large-line-height) var(--md-sys-typescale-label-large-font)",
    letterSpacing: "var(--md-sys-typescale-label-large-tracking)",
    transition: "box-shadow var(--md-sys-motion-duration-short3) var(--md-sys-motion-easing-standard)",
  };
  const variants = {
    filled:   { background: "var(--md-sys-color-primary)", color: "var(--md-sys-color-on-primary)" },
    tonal:    { background: "var(--md-sys-color-secondary-container)", color: "var(--md-sys-color-on-secondary-container)" },
    elevated: { background: "var(--md-sys-color-surface-container-low)", color: "var(--md-sys-color-primary)", boxShadow: "var(--md-sys-elevation-level1)" },
    outlined: { background: "transparent", color: "var(--md-sys-color-primary)", border: "1px solid var(--md-sys-color-outline)" },
    text:     { background: "transparent", color: "var(--md-sys-color-primary)", padding: "0 12px" },
  };
  return (
    <button onClick={onClick} style={{ ...base, ...variants[variant], ...style }}>
      {icon ? <span className="material-symbols-outlined" style={{ fontSize: 18 }}>{icon}</span> : null}
      {children}
    </button>
  );
}

// --- Icon Button ---
function IconBtn({ icon, variant = "standard", onClick, style, "aria-label": al }) {
  const base = {
    width: 40, height: 40, borderRadius: 9999, border: "none", cursor: "pointer",
    display: "inline-flex", alignItems: "center", justifyContent: "center",
  };
  const variants = {
    standard: { background: "transparent", color: "var(--md-sys-color-on-surface-variant)" },
    filled:   { background: "var(--md-sys-color-primary)", color: "var(--md-sys-color-on-primary)" },
    tonal:    { background: "var(--md-sys-color-secondary-container)", color: "var(--md-sys-color-on-secondary-container)" },
    outlined: { background: "transparent", color: "var(--md-sys-color-on-surface-variant)", border: "1px solid var(--md-sys-color-outline)" },
  };
  return (
    <button aria-label={al} onClick={onClick} style={{ ...base, ...variants[variant], ...style }}>
      <span className="material-symbols-outlined">{icon}</span>
    </button>
  );
}

// --- FAB ---
function Fab({ icon, label, size = "md", onClick }) {
  const sizes = {
    sm: { width: 40, height: 40, borderRadius: 12 },
    md: { width: 56, height: 56, borderRadius: 16 },
    lg: { width: 96, height: 96, borderRadius: 28 },
  };
  const isExt = !!label;
  return (
    <button onClick={onClick} style={{
      ...(isExt ? { height: 56, padding: "0 20px", borderRadius: 16 } : sizes[size]),
      display: "inline-flex", alignItems: "center", justifyContent: "center", gap: 12,
      background: "var(--md-sys-color-primary-container)", color: "var(--md-sys-color-on-primary-container)",
      boxShadow: "var(--md-sys-elevation-level3)", border: "none", cursor: "pointer",
      font: "500 14px/1 var(--md-ref-typeface-plain)", letterSpacing: "0.00625rem",
    }}>
      <span className="material-symbols-outlined">{icon}</span>
      {label}
    </button>
  );
}

// --- Card ---
function Card({ variant = "filled", children, style, onClick }) {
  const variants = {
    filled:   { background: "var(--md-sys-color-surface-container-highest)" },
    elevated: { background: "var(--md-sys-color-surface-container-low)", boxShadow: "var(--md-sys-elevation-level1)" },
    outlined: { background: "var(--md-sys-color-surface)", border: "1px solid var(--md-sys-color-outline-variant)" },
  };
  return (
    <div onClick={onClick} style={{
      borderRadius: "var(--md-sys-shape-corner-medium)", padding: 16,
      ...variants[variant], cursor: onClick ? "pointer" : "default", ...style,
    }}>
      {children}
    </div>
  );
}

// --- Chip ---
function Chip({ children, selected, leading, trailing, onClick }) {
  return (
    <div onClick={onClick} style={{
      display: "inline-flex", alignItems: "center", gap: 8, height: 32, padding: "0 12px",
      borderRadius: "var(--md-sys-shape-corner-small)",
      font: "500 14px/1 var(--md-ref-typeface-plain)", letterSpacing: "0.00625rem",
      background: selected ? "var(--md-sys-color-secondary-container)" : "var(--md-sys-color-surface)",
      color: selected ? "var(--md-sys-color-on-secondary-container)" : "var(--md-sys-color-on-surface-variant)",
      border: selected ? "none" : "1px solid var(--md-sys-color-outline-variant)",
      cursor: "pointer",
    }}>
      {leading ? <span className="material-symbols-outlined" style={{ fontSize: 18 }}>{leading}</span> : null}
      {children}
      {trailing ? <span className="material-symbols-outlined" style={{ fontSize: 18 }}>{trailing}</span> : null}
    </div>
  );
}

// --- Switch ---
function Switch({ checked, onChange }) {
  return (
    <div onClick={() => onChange(!checked)} style={{
      width: 52, height: 32, borderRadius: 9999, position: "relative", cursor: "pointer",
      background: checked ? "var(--md-sys-color-primary)" : "var(--md-sys-color-surface-container-highest)",
      border: checked ? "none" : "2px solid var(--md-sys-color-outline)",
      transition: "background var(--md-sys-motion-duration-short3) var(--md-sys-motion-easing-standard)",
    }}>
      <div style={{
        position: "absolute", top: "50%", transform: "translateY(-50%)",
        left: checked ? 24 : 8,
        width: checked ? 24 : 16, height: checked ? 24 : 16, borderRadius: 9999,
        background: checked ? "var(--md-sys-color-on-primary)" : "var(--md-sys-color-outline)",
        display: "flex", alignItems: "center", justifyContent: "center",
        transition: "all var(--md-sys-motion-duration-short3) var(--md-sys-motion-easing-standard)",
      }}>
        {checked ? <span className="material-symbols-outlined" style={{ fontSize: 16, color: "var(--md-sys-color-primary)" }}>check</span> : null}
      </div>
    </div>
  );
}

// --- Text Field (outlined) ---
function TextField({ label, value, onChange, type = "text", error }) {
  const [focused, setFocused] = useState(false);
  const borderColor = error ? "var(--md-sys-color-error)" : focused ? "var(--md-sys-color-primary)" : "var(--md-sys-color-outline)";
  return (
    <div style={{ position: "relative", padding: focused ? "7px 15px" : "8px 16px",
                  border: `${focused ? 2 : 1}px solid ${borderColor}`,
                  borderRadius: "var(--md-sys-shape-corner-extra-small)" }}>
      <div style={{ position: "absolute", top: -8, left: 12, padding: "0 4px",
                    background: "var(--md-sys-color-surface)",
                    font: "400 12px/1 var(--md-ref-typeface-plain)",
                    color: borderColor }}>{label}</div>
      <input value={value} type={type} onChange={(e) => onChange?.(e.target.value)}
             onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
             style={{ width: "100%", border: "none", background: "transparent", outline: "none",
                      color: "var(--md-sys-color-on-surface)",
                      font: "400 16px/24px var(--md-ref-typeface-plain)" }} />
    </div>
  );
}

// --- List item ---
function ListItem({ leading, headline, supporting, trailing, onClick }) {
  return (
    <div onClick={onClick} style={{
      display: "flex", alignItems: "center", gap: 16, padding: "12px 16px",
      cursor: onClick ? "pointer" : "default", minHeight: 56,
    }}>
      {leading ? <div style={{ width: 40, height: 40, borderRadius: 9999, background: "var(--md-sys-color-primary-container)",
                                color: "var(--md-sys-color-on-primary-container)",
                                display: "flex", alignItems: "center", justifyContent: "center", flex: "0 0 auto" }}>
        {typeof leading === "string"
          ? <span className="material-symbols-outlined">{leading}</span>
          : leading}
      </div> : null}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ font: "var(--md-sys-typescale-body-large-weight) var(--md-sys-typescale-body-large-size)/var(--md-sys-typescale-body-large-line-height) var(--md-sys-typescale-body-large-font)",
                      color: "var(--md-sys-color-on-surface)",
                      whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{headline}</div>
        {supporting ? <div style={{ font: "400 14px/20px var(--md-ref-typeface-plain)",
                                     color: "var(--md-sys-color-on-surface-variant)",
                                     whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{supporting}</div> : null}
      </div>
      {trailing ? <div style={{ color: "var(--md-sys-color-on-surface-variant)",
                                 font: "400 12px/16px var(--md-ref-typeface-plain)" }}>{trailing}</div> : null}
    </div>
  );
}

// --- Top App Bar ---
function TopAppBar({ title, leading = "menu", onLeading, actions = [] }) {
  return (
    <div style={{ height: 64, padding: "0 4px 0 16px", display: "flex", alignItems: "center", gap: 4,
                  background: "var(--md-sys-color-surface)" }}>
      <IconBtn icon={leading} onClick={onLeading} aria-label="menu" style={{ width: 48, height: 48 }} />
      <div style={{ flex: 1,
                    font: "var(--md-sys-typescale-title-large-weight) var(--md-sys-typescale-title-large-size)/var(--md-sys-typescale-title-large-line-height) var(--md-sys-typescale-title-large-font)",
                    color: "var(--md-sys-color-on-surface)" }}>{title}</div>
      {actions.map((a, i) => <IconBtn key={i} icon={a.icon} onClick={a.onClick} style={{ width: 48, height: 48 }} aria-label={a.label} />)}
    </div>
  );
}

// --- Nav Bar (bottom) ---
function NavBar({ items, active, onChange }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: `repeat(${items.length}, 1fr)`,
                  background: "var(--md-sys-color-surface-container)", padding: "12px 0 16px" }}>
      {items.map((it, i) => {
        const isActive = i === active;
        return (
          <div key={i} onClick={() => onChange(i)}
               style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4, cursor: "pointer" }}>
            <div style={{ width: 64, height: 32, borderRadius: 9999, display: "flex", alignItems: "center", justifyContent: "center",
                          background: isActive ? "var(--md-sys-color-secondary-container)" : "transparent" }}>
              <span className="material-symbols-outlined"
                    style={{ fontSize: 24,
                             color: isActive ? "var(--md-sys-color-on-secondary-container)" : "var(--md-sys-color-on-surface-variant)",
                             fontVariationSettings: isActive ? "'FILL' 1" : undefined }}>{it.icon}</span>
            </div>
            <div style={{ font: "500 12px/16px var(--md-ref-typeface-plain)",
                          color: isActive ? "var(--md-sys-color-on-surface)" : "var(--md-sys-color-on-surface-variant)" }}>{it.label}</div>
          </div>
        );
      })}
    </div>
  );
}

Object.assign(window, { Button, IconBtn, Fab, Card, Chip, Switch, TextField, ListItem, TopAppBar, NavBar });
