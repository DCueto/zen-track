/* global React, Card, Chip, Fab, IconBtn, ListItem, NavBar, TopAppBar */
const { useState: useStateInbox } = React;

function Inbox({ onCompose }) {
  const [filter, setFilter] = useStateInbox(0);
  const [active, setActive] = useStateInbox(0);

  const messages = [
    { from: "Priya Singh", subject: "Re: Q3 design review", body: "Thanks — see notes inline. Should we ship the Expressive variant?", time: "2:14 PM", unread: true, color: "#4F378B" },
    { from: "Aiden Patel", subject: "Welcome to the team", body: "Glad to have you on board. Here's the onboarding checklist for the first week.", time: "12:42 PM", unread: true, color: "#7D5260" },
    { from: "Material newsletter", subject: "What's new in M3 Expressive", body: "Spring physics, larger corner radii, and emphasized typography roll out across…", time: "9:08 AM", unread: false, color: "#625B71" },
    { from: "Calendar", subject: "Reminder: Design crit Friday 3pm", body: "Bring 3 directions for the inbox redesign.", time: "Mon", unread: false, color: "#6750A4" },
    { from: "Claude", subject: "Your weekly recap", body: "12 designs reviewed, 3 prototypes built, 1 design system created.", time: "Sun", unread: false, color: "#7D5260" },
  ];

  const filters = ["All", "Unread", "Starred", "Mentions"];
  const visible = filter === 1 ? messages.filter((m) => m.unread) : messages;

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%",
                  background: "var(--md-sys-color-surface)", color: "var(--md-sys-color-on-surface)" }}>
      <TopAppBar title="Inbox" leading="menu"
        actions={[{ icon: "search", label: "Search" }, { icon: "more_vert", label: "More" }]} />
      <div style={{ display: "flex", gap: 8, padding: "8px 16px 12px", overflowX: "auto" }}>
        {filters.map((f, i) => (
          <Chip key={i} selected={filter === i} leading={filter === i ? "check" : null}
                onClick={() => setFilter(i)}>{f}</Chip>
        ))}
      </div>
      <div style={{ flex: 1, overflow: "auto" }}>
        {visible.map((m, i) => (
          <div key={i} style={{ background: m.unread ? "var(--md-sys-color-surface-container-low)" : "transparent" }}>
            <ListItem
              leading={
                <div style={{ width: 40, height: 40, borderRadius: 9999, background: m.color, color: "#fff",
                              display: "flex", alignItems: "center", justifyContent: "center",
                              font: "500 16px/1 var(--md-ref-typeface-plain)" }}>
                  {m.from.split(" ").map(s => s[0]).join("").slice(0, 2)}
                </div>
              }
              headline={<span style={{ fontWeight: m.unread ? 500 : 400 }}>{m.from} — {m.subject}</span>}
              supporting={m.body}
              trailing={m.time}
            />
          </div>
        ))}
      </div>
      <div style={{ position: "absolute", bottom: 96, right: 24 }}>
        <Fab icon="edit" label="Compose" onClick={onCompose} />
      </div>
      <NavBar
        items={[
          { icon: "inbox", label: "Inbox" },
          { icon: "stars", label: "Starred" },
          { icon: "send", label: "Sent" },
          { icon: "folder", label: "Folders" },
        ]}
        active={active} onChange={setActive}
      />
    </div>
  );
}

window.Inbox = Inbox;
