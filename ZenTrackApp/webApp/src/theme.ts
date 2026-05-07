import { createTheme, type Theme } from '@mui/material/styles';

// M3 color roles derived from seed #5B6CF9 (do not use the seed directly in components)
// Generated from Material Theme Builder: https://material-foundation.github.io/material-theme-builder/

const lightPalette = {
  primary:           { main: '#4C54C0', light: '#8479FA', dark: '#3832A0', contrastText: '#FFFFFF' },
  secondary:         { main: '#5C5D72', light: '#8385A9', dark: '#404154', contrastText: '#FFFFFF' },
  error:             { main: '#BA1A1A', light: '#D4453F', dark: '#8A0E0E', contrastText: '#FFFFFF' },
  background:        { default: '#FEFBFF', paper: '#F4EFF7' },
  text:              { primary: '#1B1B1F', secondary: '#46464F' },
};

const darkPalette = {
  primary:           { main: '#C0C1FF', light: '#E1E0FF', dark: '#8479FA', contrastText: '#1F1D7A' },
  secondary:         { main: '#C6C4DD', light: '#E2E0F9', dark: '#8385A9', contrastText: '#2E2D42' },
  error:             { main: '#FFB4AB', light: '#FFDAD6', dark: '#93000A', contrastText: '#690005' },
  background:        { default: '#1C1B1F', paper: '#2B2930' },
  text:              { primary: '#E5E1E6', secondary: '#CAC4CF' },
};

const sharedTheme = {
  typography: {
    fontFamily: '"Roboto Flex", "Roboto", sans-serif',
  },
  shape: { borderRadius: 12 },
};

export const lightTheme: Theme = createTheme({
  ...sharedTheme,
  palette: { mode: 'light', ...lightPalette },
});

export const darkTheme: Theme = createTheme({
  ...sharedTheme,
  palette: { mode: 'dark', ...darkPalette },
});
