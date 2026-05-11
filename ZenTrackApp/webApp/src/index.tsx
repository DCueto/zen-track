import React from 'react';
import ReactDOM from 'react-dom/client';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import useMediaQuery from '@mui/material/useMediaQuery';
import { lightTheme, darkTheme } from './theme';
import { AuthScreen } from './screens/AuthScreen';
import { OAuthCallbackScreen } from './screens/OAuthCallbackScreen';
import { useAuthStore } from './store/useAuthStore';

function App() {
  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)');
  const theme = prefersDark ? darkTheme : lightTheme;
  const token = useAuthStore((s) => s.token);

  const screen = () => {
    if (window.location.pathname === '/auth/callback') return <OAuthCallbackScreen />;
    if (token == null) return <AuthScreen />;
    // WorkspaceScreen — siguiente tarea
    return <div style={{ padding: 32 }}>Workspaces (próximamente)</div>;
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {screen()}
    </ThemeProvider>
  );
}

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Failed to find the root element');

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
