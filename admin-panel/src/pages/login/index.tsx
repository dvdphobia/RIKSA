import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  Check,
  Copy,
  ExternalLink,
  KeyRound,
  Loader2,
  RefreshCw,
  Send,
  ShieldCheck,
} from "lucide-react";
import { type FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router";
import { ApiError } from "../../lib/api";
import { useAuth } from "../../lib/auth";
import type { TelegramAuthStartResponse } from "../../lib/types";
import { Alert, AlertDescription } from "../../components/ui/alert";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";

function formatRemaining(seconds: number): string {
  const clamped = Math.max(0, Math.floor(seconds));
  const mins = Math.floor(clamped / 60);
  const secs = clamped % 60;
  return `${mins}:${secs.toString().padStart(2, "0")}`;
}

export const Login = () => {
  const {
    user,
    getTelegramBotInfo,
    startTelegramAuth,
    checkTelegramStatus,
    loginWithTelegramCode,
  } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from =
    (location.state as { from?: { pathname: string } } | null)?.from
      ?.pathname ?? "/";

  const [tab, setTab] = useState<"link" | "code">("link");
  const [error, setError] = useState<string | null>(null);

  // One-tap Telegram flow state (kept strictly in memory)
  const [challenge, setChallenge] = useState<TelegramAuthStartResponse | null>(
    null,
  );
  const [isStarting, setIsStarting] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState<number>(0);
  const [copied, setCopied] = useState(false);
  const pollingInFlightRef = useRef(false);

  // 8-character code flow state
  const [code, setCode] = useState("");
  const [isRedeeming, setIsRedeeming] = useState(false);

  const botInfoQuery = useQuery({
    queryKey: ["telegram-bot-info"],
    queryFn: getTelegramBotInfo,
    retry: 1,
  });

  const botInfo = botInfoQuery.data;
  const botUnavailable = botInfo !== undefined && !botInfo.available;

  // Countdown timer for active one-tap challenge
  useEffect(() => {
    if (!challenge) {
      setSecondsLeft(0);
      return;
    }
    const updateRemaining = () => {
      const diffSec = Math.ceil(
        (new Date(challenge.expiresAt).getTime() - Date.now()) / 1000,
      );
      if (diffSec <= 0) {
        setSecondsLeft(0);
        setChallenge(null);
        setError("Sign-in request expired. Please start again.");
      } else {
        setSecondsLeft(diffSec);
      }
    };
    updateRemaining();
    const timer = window.setInterval(updateRemaining, 1000);
    return () => window.clearInterval(timer);
  }, [challenge]);

  const pollOnce = useCallback(
    async (requestToken: string) => {
      if (pollingInFlightRef.current) return;
      pollingInFlightRef.current = true;
      try {
        const res = await checkTelegramStatus(requestToken);
        if (res.completed) {
          setChallenge(null);
          navigate(from, { replace: true });
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 410 && user) {
          return;
        }
        setChallenge(null);
        setError(err instanceof Error ? err.message : "Telegram sign-in failed.");
      } finally {
        pollingInFlightRef.current = false;
      }
    },
    [checkTelegramStatus, from, navigate, user],
  );

  // Poll /auth/telegram/status on interval and on window focus/visibility
  useEffect(() => {
    if (!challenge) return;
    const token = challenge.requestToken;
    const intervalMs = Math.max(2, challenge.pollIntervalSeconds || 3) * 1000;

    const intervalId = window.setInterval(() => {
      void pollOnce(token);
    }, intervalMs);

    const onFocusOrVisible = () => {
      if (document.visibilityState === "visible") {
        void pollOnce(token);
      }
    };

    window.addEventListener("focus", onFocusOrVisible);
    document.addEventListener("visibilitychange", onFocusOrVisible);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", onFocusOrVisible);
      document.removeEventListener("visibilitychange", onFocusOrVisible);
    };
  }, [challenge, pollOnce]);

  if (user && user.role === "ADMIN") {
    return <Navigate to={from} replace />;
  }

  const handleStartTelegram = async () => {
    setError(null);
    setIsStarting(true);
    try {
      const started = await startTelegramAuth();
      setChallenge(started);
      window.open(started.botUrl, "_blank", "noopener,noreferrer");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to start Telegram sign-in.",
      );
    } finally {
      setIsStarting(false);
    }
  };

  const handleCopyLink = async () => {
    if (!challenge) return;
    try {
      await navigator.clipboard.writeText(challenge.botUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // Ignore clipboard failure
    }
  };

  const handleCodeSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = code.trim();
    if (!trimmed) {
      setError("Enter the 8-character code from the Telegram bot.");
      return;
    }
    setError(null);
    setIsRedeeming(true);
    try {
      await loginWithTelegramCode(trimmed);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to verify code.");
    } finally {
      setIsRedeeming(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <div className="w-full max-w-md space-y-6 rounded-lg border bg-card p-8 shadow-sm">
        <div className="space-y-1.5 text-center">
          <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-md bg-primary font-bold text-primary-foreground">
            R
          </div>
          <h1 className="text-xl font-semibold">Riksa Admin</h1>
          <p className="text-sm text-muted-foreground">
            Sign in with a Telegram account linked to an administrator profile
          </p>
        </div>

        <div className="flex items-center justify-center gap-1.5 rounded-md border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
          <ShieldCheck className="h-4 w-4 shrink-0 text-primary" />
          <span>Administrator-only access enforced via Telegram</span>
        </div>

        {botUnavailable && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              Telegram sign-in is currently unavailable on the server. Ensure{" "}
              <code className="font-mono text-xs">TELEGRAM_ENABLED=true</code>{" "}
              and bot credentials are configured.
            </AlertDescription>
          </Alert>
        )}

        {error && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <Tabs
          value={tab}
          onValueChange={(v) => {
            setTab(v as "link" | "code");
            setError(null);
          }}
          className="w-full"
        >
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="link">
              <Send className="mr-1.5 h-3.5 w-3.5" />
              Open Telegram
            </TabsTrigger>
            <TabsTrigger value="code">
              <KeyRound className="mr-1.5 h-3.5 w-3.5" />
              Log in with code
            </TabsTrigger>
          </TabsList>

          <TabsContent value="link" className="mt-4 space-y-4">
            {!challenge ? (
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  Open the Riksa Telegram bot and tap{" "}
                  <span className="font-medium text-foreground">
                    Confirm sign-in
                  </span>
                  . Your session will start automatically once confirmed.
                </p>
                <Button
                  type="button"
                  className="w-full"
                  disabled={isStarting || botUnavailable}
                  onClick={handleStartTelegram}
                >
                  {isStarting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Starting request...
                    </>
                  ) : (
                    <>
                      <Send className="mr-2 h-4 w-4" />
                      Continue with Telegram
                    </>
                  )}
                </Button>
              </div>
            ) : (
              <div className="space-y-4 rounded-md border bg-muted/20 p-4">
                <div className="flex items-start gap-3">
                  <Loader2 className="mt-0.5 h-5 w-5 shrink-0 animate-spin text-primary" />
                  <div className="space-y-1 text-sm">
                    <p className="font-medium">
                      Waiting for confirmation in Telegram...
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Open{" "}
                      <span className="font-mono font-medium text-foreground">
                        @{challenge.botUsername}
                      </span>{" "}
                      and tap{" "}
                      <span className="font-medium text-foreground">
                        Confirm sign-in
                      </span>
                      . Expires in{" "}
                      <span className="font-mono font-medium text-foreground">
                        {formatRemaining(secondsLeft)}
                      </span>
                      .
                    </p>
                  </div>
                </div>

                <Button asChild className="w-full">
                  <a
                    href={challenge.botUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <ExternalLink className="mr-2 h-4 w-4" />
                    Open @{challenge.botUsername}
                  </a>
                </Button>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="flex-1"
                    onClick={handleCopyLink}
                  >
                    {copied ? (
                      <>
                        <Check className="mr-1.5 h-3.5 w-3.5" />
                        Copied link
                      </>
                    ) : (
                      <>
                        <Copy className="mr-1.5 h-3.5 w-3.5" />
                        Copy link
                      </>
                    )}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setChallenge(null);
                      setError(null);
                    }}
                  >
                    <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
                    Start over
                  </Button>
                </div>
              </div>
            )}
          </TabsContent>

          <TabsContent value="code" className="mt-4">
            <form onSubmit={handleCodeSubmit} className="space-y-4">
              <p className="text-sm text-muted-foreground">
                Open{" "}
                {botInfo?.botUrl ? (
                  <a
                    href={botInfo.botUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-mono font-medium text-primary underline-offset-4 hover:underline"
                  >
                    @{botInfo.botUsername}
                  </a>
                ) : (
                  <span className="font-medium text-foreground">
                    the Riksa Telegram bot
                  </span>
                )}{" "}
                and send <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">/code</code>{" "}
                to receive a single-use {botInfo?.codeLength ?? 8}-character sign-in code.
              </p>

              <div className="space-y-2">
                <Label htmlFor="telegram-code">Telegram sign-in code</Label>
                <Input
                  id="telegram-code"
                  placeholder="VE9B-LZ8J"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  maxLength={12}
                  autoComplete="one-time-code"
                  className="text-center font-mono text-base uppercase tracking-widest"
                  disabled={isRedeeming || botUnavailable}
                />
              </div>

              <Button
                type="submit"
                className="w-full"
                disabled={isRedeeming || !code.trim() || botUnavailable}
              >
                {isRedeeming ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Verifying code...
                  </>
                ) : (
                  "Verify & sign in"
                )}
              </Button>
            </form>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};
