/* global React, Button, Chip, IconBtn, TextField, TopAppBar */
const { useState: useStateCompose } = React;

function Compose({ onClose, onSend }) {
  const [to, setTo] = useStateCompose("priya@example.com");
  const [subject, setSubject] = useStateCompose("Re: Q3 design review");
  const [body, setBody] = useStateCompose("Thanks for the notes — let's go with direction B and ship Friday.");
  const [recipients] = useStateCompose(["Priya Singh", "Aiden Patel"]);

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%",
                  background: "var(--md-sys-color-surface)", color: "var(--md-sys-color-on-surface)" }}>
      <div style={{ height: 64, padding: "0 4px 0 4px", display: "flex", alignItems: "center", gap: 4,
                    background: "var(--md-sys-color-surface)" }}>
        <IconBtn icon="close" onClick={onClose} style={{ width: 48, height: 48 }} aria-label="Close" />
        <div style={{ flex: 1, paddingLeft: 12,
                      font: "var(--md-sys-typescale-title-large-weight) var(--md-sys-typescale-title-large-size)/var(--md-sys-typescale-title-large-line-height) var(--md-sys-typescale-title-large-font)" }}>New message</div>
        <IconBtn icon="attach_file" style={{ width: 48, height: 48 }} aria-label="Attach" />
        <div style={{ paddingRight: 8 }}>
          <Button variant="filled" icon="send" onClick={onSend}>Send</Button>
        </div>
      </div>

      <div style={{ flex: 1, overflow: "auto", padding: "16px 16px 80px", display: "flex", flexDirection: "column", gap: 16 }}>
        <TextField label="From" value="aiden@example.com" />
        <div>
          <TextField label="To" value={to} onChange={setTo} />
          <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 8 }}>
            {recipients.map((r) => (
              <Chip key={r} trailing="close" selected>{r}</Chip>
            ))}
          </div>
        </div>
        <TextField label="Subject" value={subject} onChange={setSubject} />
        <div style={{ position: "relative", padding: "12px 16px",
                      border: "1px solid var(--md-sys-color-outline)",
                      borderRadius: "var(--md-sys-shape-corner-extra-small)", minHeight: 200 }}>
          <div style={{ position: "absolute", top: -8, left: 12, padding: "0 4px",
                        background: "var(--md-sys-color-surface)",
                        font: "400 12px/1 var(--md-ref-typeface-plain)",
                        color: "var(--md-sys-color-on-surface-variant)" }}>Message</div>
          <textarea value={body} onChange={(e) => setBody(e.target.value)}
                    style={{ width: "100%", minHeight: 180, border: "none", background: "transparent",
                             outline: "none", color: "var(--md-sys-color-on-surface)", resize: "vertical",
                             font: "400 16px/24px var(--md-ref-typeface-plain)" }} />
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <Chip leading="event">Schedule send</Chip>
          <Chip leading="format_paint">Formatting</Chip>
          <Chip leading="image">Insert image</Chip>
        </div>
      </div>
    </div>
  );
}

window.Compose = Compose;
