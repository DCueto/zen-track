import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Divider,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useAuthStore } from '../store/useAuthStore';

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
      <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
      <path fill="#FF3D00" d="M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"/>
      <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
      <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303c-.792 2.237-2.231 4.166-4.087 5.571l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
    </svg>
  );
}

interface TabPanelProps {
  children: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, index, value }: TabPanelProps) {
  return (
    <div hidden={value !== index} role="tabpanel" id={`auth-tabpanel-${index}`}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

export function AuthScreen() {
  const [tab, setTab] = useState(0);

  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');

  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regPasswordConfirm, setRegPasswordConfirm] = useState('');
  const [confirmError, setConfirmError] = useState('');

  const login = useAuthStore((s) => s.login);
  const register = useAuthStore((s) => s.register);
  const clearError = useAuthStore((s) => s.clearError);
  const isLoading = useAuthStore((s) => s.isLoading);
  const error = useAuthStore((s) => s.error);

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    clearError();
    setConfirmError('');
    setTab(newValue);
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    await login(loginEmail, loginPassword);
  };

  const handleGoogleLogin = () => {
    window.location.href = `${import.meta.env.VITE_API_BASE_URL}/api/auth/google`;
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (regPassword !== regPasswordConfirm) {
      setConfirmError('Las contraseñas no coinciden');
      return;
    }
    setConfirmError('');
    await register(regName, regEmail, regPassword);
  };

  return (
    <Box
      sx={{
        width: '100%',
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        backgroundImage: (theme) => [
          `radial-gradient(ellipse 70% 55% at 15% 0%,   ${alpha(theme.palette.primary.main,   theme.palette.mode === 'dark' ? 0.22 : 0.11)} 0%, transparent 60%)`,
          `radial-gradient(ellipse 60% 50% at 85% 100%, ${alpha(theme.palette.secondary.main, theme.palette.mode === 'dark' ? 0.16 : 0.09)} 0%, transparent 55%)`,
        ].join(', '),
        px: 2,
        py: 6,
      }}
    >
      <Box sx={{ width: '100%', maxWidth: 400 }}>

        {/* Logo */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 6 }}>
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: 1.5,
              bgcolor: 'primary.main',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Typography sx={{ color: 'primary.contrastText', fontWeight: 800, fontSize: 13, letterSpacing: 0.5 }}>
              ZT
            </Typography>
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 700, color: 'text.primary', letterSpacing: -0.3 }}>
            ZenTrack
          </Typography>
        </Box>

        {/* Heading */}
        <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary', mb: 0.75 }}>
          {tab === 0 ? 'Bienvenido de vuelta' : 'Crear una cuenta'}
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 4 }}>
          {tab === 0
            ? 'Accede a tu workspace de ZenTrack.'
            : 'Únete a ZenTrack y empieza a gestionar proyectos.'}
        </Typography>

        {/* Tabs */}
        <Tabs
          value={tab}
          onChange={handleTabChange}
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab
            label="Iniciar sesión"
            id="auth-tab-0"
            sx={{ fontWeight: 600, textTransform: 'none', fontSize: 14, pb: 1.5 }}
          />
          <Tab
            label="Registrarse"
            id="auth-tab-1"
            sx={{ fontWeight: 600, textTransform: 'none', fontSize: 14, pb: 1.5 }}
          />
        </Tabs>

        {error && (
          <Alert severity="error" sx={{ mt: 3 }}>
            {error}
          </Alert>
        )}

        {/* Login form */}
        <TabPanel value={tab} index={0}>
          <Box
            component="form"
            onSubmit={handleLogin}
            sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}
          >
            <TextField
              label="Email"
              type="email"
              value={loginEmail}
              onChange={(e) => setLoginEmail(e.target.value)}
              required
              fullWidth
              autoComplete="email"
            />
            <TextField
              label="Contraseña"
              type="password"
              value={loginPassword}
              onChange={(e) => setLoginPassword(e.target.value)}
              required
              fullWidth
              autoComplete="current-password"
            />
            <Button
              type="submit"
              variant="contained"
              fullWidth
              loading={isLoading}
              size="large"
              sx={{ mt: 0.5, py: 1.5, fontWeight: 600, textTransform: 'none', fontSize: 15, borderRadius: 2 }}
            >
              Iniciar sesión
            </Button>
          </Box>
        </TabPanel>

        {/* Register form */}
        <TabPanel value={tab} index={1}>
          <Box
            component="form"
            onSubmit={handleRegister}
            sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}
          >
            <TextField
              label="Nombre completo"
              value={regName}
              onChange={(e) => setRegName(e.target.value)}
              required
              fullWidth
              autoComplete="name"
            />
            <TextField
              label="Email"
              type="email"
              value={regEmail}
              onChange={(e) => setRegEmail(e.target.value)}
              required
              fullWidth
              autoComplete="email"
            />
            <TextField
              label="Contraseña"
              type="password"
              value={regPassword}
              onChange={(e) => setRegPassword(e.target.value)}
              required
              fullWidth
              autoComplete="new-password"
            />
            <TextField
              label="Confirmar contraseña"
              type="password"
              value={regPasswordConfirm}
              onChange={(e) => setRegPasswordConfirm(e.target.value)}
              required
              fullWidth
              autoComplete="new-password"
              error={!!confirmError}
              helperText={confirmError}
            />
            <Button
              type="submit"
              variant="contained"
              fullWidth
              loading={isLoading}
              size="large"
              sx={{ mt: 0.5, py: 1.5, fontWeight: 600, textTransform: 'none', fontSize: 15, borderRadius: 2 }}
            >
              Crear cuenta
            </Button>
          </Box>
        </TabPanel>

        <Divider sx={{ mt: 3, mb: 2 }}>
          <Typography variant="caption" sx={{ color: 'text.secondary', px: 1 }}>
            o continúa con
          </Typography>
        </Divider>

        <Button
          variant="outlined"
          fullWidth
          size="large"
          onClick={handleGoogleLogin}
          startIcon={<GoogleIcon />}
          sx={{ py: 1.5, fontWeight: 600, textTransform: 'none', fontSize: 15, borderRadius: 2 }}
        >
          Continuar con Google
        </Button>

        <Divider sx={{ mt: 4, mb: 3 }} />
        <Typography variant="caption" sx={{ color: 'text.disabled', display: 'block', textAlign: 'center' }}>
          ZenTrack · Gestión ágil, sin ruido.
        </Typography>

      </Box>
    </Box>
  );
}
