import "../global.css";
import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';
import * as SplashScreen from 'expo-splash-screen';
import 'react-native-reanimated';

import { useColorScheme } from '@/hooks/use-color-scheme';
import { GuardianProvider, useGuardian } from '@/context/GuardianContext';

// Keep the splash screen visible while we fetch resources
SplashScreen.preventAutoHideAsync().catch(() => {});

function RootLayoutContent() {
  const colorScheme = useColorScheme();
  const { isInitialized } = useGuardian();
  const [isAppReady, setIsAppReady] = useState(false);

  useEffect(() => {
    // App initialization timeout to prevent freeze
    const timeout = setTimeout(() => {
      setIsAppReady(true);
    }, 3000);

    if (isInitialized) {
      clearTimeout(timeout);
      setIsAppReady(true);
    }

    return () => clearTimeout(timeout);
  }, [isInitialized]);

  useEffect(() => {
    if (isAppReady) {
      SplashScreen.hideAsync().catch(() => {});
    }
  }, [isAppReady]);

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="modal" options={{ presentation: 'modal', title: 'Modal' }} />
      </Stack>
      <StatusBar style="auto" />
    </ThemeProvider>
  );
}

export default function RootLayout() {
  return (
    <GuardianProvider>
      <RootLayoutContent />
    </GuardianProvider>
  );
}
