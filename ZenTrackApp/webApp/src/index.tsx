import React from 'react';
import ReactDOM from 'react-dom/client';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import useMediaQuery from '@mui/material/useMediaQuery';
import { lightTheme, darkTheme } from './theme';
import { AuthScreen } from './screens/AuthScreen';
import { useAuthStore } from './store/useAuthStore';

function App() {
  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)');
  const theme = prefersDark ? darkTheme : lightTheme;
  const token = useAuthStore((s) => s.token);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {token == null ? (
        <AuthScreen />
      ) : (
        // WorkspaceScreen — siguiente tarea
        <div style={{ padding: 32 }}>Workspaces (próximamente)</div>
      )}
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
