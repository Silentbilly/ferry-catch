export type DateInput = string | number | Date | null | undefined;

export function parseDate(input: DateInput): Date | null {
  if (input == null || input === "") return null;

  const d = input instanceof Date ? input : new Date(input);
  return Number.isNaN(d.getTime()) ? null : d;
}

const YMD_2DIGIT: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
};

export function formatHHmm(
  input: DateInput,
  mode: "local" | "utc" = "local",
  locales?: Intl.LocalesArgument,
): string {
  const d = parseDate(input);
  if (!d) return "";

  const options: Intl.DateTimeFormatOptions = {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    ...(mode === "utc" ? { timeZone: "UTC" } : {}),
  };

  return new Intl.DateTimeFormat(locales, options).format(d);
}

export function formatYYYYMMDD(
  input: DateInput,
  mode: "local" | "utc" = "local",
): string {
  const d = parseDate(input);
  if (!d) return "";

  const year = mode === "utc" ? d.getUTCFullYear() : d.getFullYear();
  const month = (mode === "utc" ? d.getUTCMonth() : d.getMonth()) + 1;
  const day = mode === "utc" ? d.getUTCDate() : d.getDate();

  const mm = String(month).padStart(2, "0");
  const dd = String(day).padStart(2, "0");
  return `${year}-${mm}-${dd}`;
}
