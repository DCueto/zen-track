import { useEffect, useState } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import { useAuthStore } from '../store/useAuthStore';
import * as authService from '../services/authService';

export function OAuthCallbackScreen() {
  const setTokenFromOAuth = useAuthStore((s) => s.setTokenFromOAuth);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');

    if (!code || !state) {
      setError('Parámetros OAuth inválidos');
      return;
    }

    authService.exchangeGoogleCode({ code, state }).then((result) => {
      if (result.success) {
        setTokenFromOAuth(result.data.token);
        window.history.replaceState({}, '', '/');
      } else {
        setError(result.error);
      }
    });
  }, []);

  if (error) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: 2 }}>
        <Typography color="error">{error}</Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          <a href="/">Volver al inicio</a>
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', gap: 2 }}>
      <CircularProgress />
      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
        Autenticando con Google…
      </Typography>
    </Box>
  );
}
