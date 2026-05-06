/* global React, Card, ListItem, Switch, TopAppBar */
const { useState: useStateSettings } = React;

function Settings({ onBack }) {
  const [notif, setNotif] = useStateSettings(true);
  const [dark, setDark] = useStateSettings(false);
  const [autosave, setAutosave] = useStateSettings(true);
  const [analytics, setAnalytics] = useStateSettings(false);

  const Section = ({ title, children }) => (
    <div style={{ padding: "12px 16px 4px" }}>
      <div style={{ font: "500 14px/20px var(--md-ref-typeface-plain)",
                    color: "var(--md-sys-color-primary)", letterSpacing: 0, marginBottom: 4 }}>{title}</div>
      <div style={{ background: "var(--md-sys-color-surface-container-low)",
                    borderRadius: "var(--md-sys-shape-corner-medium)", overflow: "hidden" }}>{children}</div>
    </div>
  );

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%",
                  background: "var(--md-sys-color-surface)", color: "var(--md-sys-color-on-surface)" }}>
      <TopAppBar title="Settings" leading="arrow_back" onLeading={onBack}
                 actions={[{ icon: "search", label: "Search" }]} />
      <div style={{ flex: 1, overflow: "auto", paddingBottom: 16 }}>
        <Section title="Account">
          <ListItem leading="account_circle" headline="Aiden Patel" supporting="aiden@example.com" trailing="" />
          <div style={{ height: 1, background: "var(--md-sys-color-outline-variant)", marginLeft: 72 }}></div>
          <ListItem leading="key" headline="Change password" supporting="Last changed 3 months ago" />
        </Section>

        <Section title="Notifications">
          <div style={{ display: "flex", alignItems: "center", padding: "12px 16px", gap: 16 }}>
            <span className="material-symbols-outlined" style={{ color: "var(--md-sys-color-on-surface-variant)" }}>notifications</span>
            <div style={{ flex: 1 }}>
              <div style={{ font: "400 16px/24px var(--md-ref-typeface-plain)" }}>Push notifications</div>
              <div style={{ font: "400 14px/20px var(--md-ref-typeface-plain)", color: "var(--md-sys-color-on-surface-variant)" }}>Banners, sounds, badges</div>
            </div>
            <Switch checked={notif} onChange={setNotif} />
          </div>
          <div style={{ height: 1, background: "var(--md-sys-color-outline-variant)", marginLeft: 56 }}></div>
          <div style={{ display: "flex", alignItems: "center", padding: "12px 16px", gap: 16 }}>
            <span className="material-symbols-outlined" style={{ color: "var(--md-sys-color-on-surface-variant)" }}>save</span>
            <div style={{ flex: 1 }}>
              <div style={{ font: "400 16px/24px var(--md-ref-typeface-plain)" }}>Auto-save drafts</div>
              <div style={{ font: "400 14px/20px var(--md-ref-typeface-plain)", color: "var(--md-sys-color-on-surface-variant)" }}>Save every 30 seconds</div>
            </div>
            <Switch checked={autosave} onChange={setAutosave} />
          </div>
        </Section>

        <Section title="Appearance">
          <div style={{ display: "flex", alignItems: "center", padding: "12px 16px", gap: 16 }}>
            <span className="material-symbols-outlined" style={{ color: "var(--md-sys-color-on-surface-variant)" }}>dark_mode</span>
            <div style={{ flex: 1 }}>
              <div style={{ font: "400 16px/24px var(--md-ref-typeface-plain)" }}>Dark theme</div>
              <div style={{ font: "400 14px/20px var(--md-ref-typeface-plain)", color: "var(--md-sys-color-on-surface-variant)" }}>Use system setting</div>
            </div>
            <Switch checked={dark} onChange={setDark} />
          </div>
        </Section>

        <Section title="Privacy">
          <div style={{ display: "flex", alignItems: "center", padding: "12px 16px", gap: 16 }}>
            <span className="material-symbols-outlined" style={{ color: "var(--md-sys-color-on-surface-variant)" }}>analytics</span>
            <div style={{ flex: 1 }}>
              <div style={{ font: "400 16px/24px var(--md-ref-typeface-plain)" }}>Share usage data</div>
              <div style={{ font: "400 14px/20px var(--md-ref-typeface-plain)", color: "var(--md-sys-color-on-surface-variant)" }}>Helps us improve the product</div>
            </div>
            <Switch checked={analytics} onChange={setAnalytics} />
          </div>
        </Section>
      </div>
    </div>
  );
}

window.Settings = Settings;
