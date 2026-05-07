import { useCallback, useEffect, useId, useMemo, useRef, useState } from "react";
import type { ChangeEvent, FormEvent, MouseEvent as ReactMouseEvent } from "react";

type CarPhoto = {
  id: number;
  url: string;
};

type Car = {
  id: number;
  brand: string;
  model: string;
  year: number;
  color: string;
  /** Тип продавца с бэкенда: PERSON | DEALERSHIP */
  sellerAccountType?: string | null;
  interiorColor?: string | null;
  interiorMaterial?: string | null;
  engineVolume?: number | null;
  mileage?: number | null;
  powerHp?: number | null;
  fuelConsumptionCity?: number | null;
  fuelConsumptionHighway?: number | null;
  fuelConsumptionMixed?: number | null;
  seatCount?: number | null;
  city?: string | null;
  transmission?: string | null;
  bodyType?: string | null;
  engineType?: string | null;
  driveType?: string | null;
  price: number;
  /** USD или BYN; старые объявления без поля считаются USD. */
  priceCurrency?: string | null;
  /** Название салона или продавца (с бэкенда). */
  sellerDisplayName?: string | null;
  /** Телефон для связи (салон или владелец). */
  sellerPhone?: string | null;
  /** ISO-8601, момент публикации объявления (UTC). */
  publishedAt?: string | null;
  featureNames: string[];
  photos?: CarPhoto[];
};

type Dealership = {
  id: number;
  name: string;
  address: string;
  phone: string;
};

type DealershipWithCars = Dealership & {
  cars: Car[];
};

const apiBaseFromEnv = import.meta.env.VITE_API_BASE_URL?.trim();
const API_BASE = (() => {
  if (!apiBaseFromEnv) {
    return import.meta.env.PROD ? `${window.location.origin}/api` : "http://localhost:8080/api";
  }
  // В production игнорируем localhost из env, чтобы хостинг не пытался ходить в локальную машину пользователя.
  if (import.meta.env.PROD) {
    try {
      const u = new URL(apiBaseFromEnv);
      if (u.hostname === "localhost" || u.hostname === "127.0.0.1") {
        return `${window.location.origin}/api`;
      }
    } catch {
      return `${window.location.origin}/api`;
    }
  }
  return apiBaseFromEnv;
})();

/** Сколько BYN за 1 USD (для показа «второй» цены; при желании задайте VITE_BYN_PER_USD в .env). */
const RAW_BYN_PER_USD = Number(import.meta.env.VITE_BYN_PER_USD);
const BYN_PER_USD =
  Number.isFinite(RAW_BYN_PER_USD) && RAW_BYN_PER_USD > 0 ? RAW_BYN_PER_USD : 3.25;

type ListingCurrency = "USD" | "BYN";

function listingCurrencyLabel(code: string | null | undefined): ListingCurrency {
  const c = (code || "USD").trim().toUpperCase();
  return c === "BYN" ? "BYN" : "USD";
}

/** Суммы в BYN и USD для отображения (основная строка всегда в рублях). */
function carDualAmounts(price: number, currencyCode: string | null | undefined): { byn: number; usd: number } {
  const cur = listingCurrencyLabel(currencyCode);
  if (cur === "BYN") {
    return { byn: price, usd: price / BYN_PER_USD };
  }
  return { byn: price * BYN_PER_USD, usd: price };
}

function PriceDualBlock({
  price,
  currencyCode,
  vat = false,
  className = "",
}: {
  price: number;
  currencyCode: string | null | undefined;
  /** Строка «… р. с НДС» вместо «… р.» */
  vat?: boolean;
  className?: string;
}) {
  const { byn, usd } = carDualAmounts(price, currencyCode);
  const bynStr = Math.round(byn).toLocaleString("ru-RU");
  const usdStr = Math.round(usd).toLocaleString("ru-RU");
  return (
    <div className={`price-dual ${className}`.trim()}>
      <span className="price-dual__primary">{vat ? `${bynStr} р. с НДС` : `${bynStr} р.`}</span>
      <span className="price-dual__secondary">≈ {usdStr} USD</span>
    </div>
  );
}

const FAVORITE_CAR_IDS_KEY = "autosalon_favorite_car_ids";

function loadFavoriteCarIds(): Set<number> {
  try {
    const raw = localStorage.getItem(FAVORITE_CAR_IDS_KEY);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return new Set();
    return new Set(
      parsed.filter((x): x is number => typeof x === "number" && Number.isFinite(x))
    );
  } catch {
    return new Set();
  }
}

function persistFavoriteCarIds(ids: Set<number>) {
  try {
    localStorage.setItem(FAVORITE_CAR_IDS_KEY, JSON.stringify([...ids]));
  } catch {
    /* ignore */
  }
}

function listingLeasingHint(car: Car): string {
  const { byn, usd } = carDualAmounts(car.price, car.priceCurrency);
  const monthlyByn = Math.max(120, Math.round(byn / 56));
  const monthlyUsd = Math.max(5, Math.round(usd / 56));
  return `Лизинг от ${monthlyByn.toLocaleString("ru-RU")} р. (≈ ${monthlyUsd.toLocaleString("ru-RU")} USD) в месяц`;
}

/** Метка времени для сортировки по дате публикации (fallback — id). */
function carPublishedSortKeyMs(car: Car): number {
  const raw = car.publishedAt?.trim();
  if (raw) {
    const n = Date.parse(raw);
    if (Number.isFinite(n)) return n;
  }
  return car.id * 86400000;
}

function formatPublishedRelative(iso: string | null | undefined): string {
  const raw = iso?.trim();
  if (!raw) return "дата публикации не указана";
  const t = Date.parse(raw);
  if (!Number.isFinite(t)) return "дата публикации не указана";
  const now = Date.now();
  const diffSec = Math.floor((now - t) / 1000);
  if (diffSec < 45) return "только что";
  if (diffSec < 0) return "только что";

  const rtf = new Intl.RelativeTimeFormat("ru", { numeric: "auto" });

  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return rtf.format(-diffMin, "minute");

  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return rtf.format(-diffHr, "hour");

  const diffDay = Math.floor(diffHr / 24);
  if (diffDay < 7) return rtf.format(-diffDay, "day");

  const diffWeek = Math.floor(diffDay / 7);
  if (diffWeek < 8) return rtf.format(-diffWeek, "week");

  const diffMonth = Math.floor(diffDay / 30);
  if (diffMonth < 24) return rtf.format(-diffMonth, "month");

  const diffYear = Math.floor(diffDay / 365);
  return rtf.format(-diffYear, "year");
}

function listingPublishedPhrase(car: Car): string {
  return formatPublishedRelative(car.publishedAt);
}

function listingSnippetText(car: Car): string {
  if (car.featureNames?.length) {
    return car.featureNames.slice(0, 4).join(", ");
  }
  const interior = car.interiorColor?.trim();
  return [car.color, interior ? `салон ${interior}` : null].filter(Boolean).join(", ") || "Подробности в карточке объявления.";
}

function listingShowTopChip(car: Car): boolean {
  if (car.sellerAccountType === "DEALERSHIP") return true;
  return car.id % 5 === 2 || car.id % 5 === 4;
}

function listingShowVatStyle(car: Car): boolean {
  return carDualAmounts(car.price, car.priceCurrency).byn >= 120_000;
}

function listingBodyWithDoors(car: Car): string {
  const code = car.bodyType?.trim().toLowerCase();
  const doors = car.seatCount != null && car.seatCount > 0 ? car.seatCount : 5;
  const fuelCode = car.engineType?.trim().toLowerCase();
  const fuel =
    fuelCode && ENGINE_LABELS[fuelCode] ? ENGINE_LABELS[fuelCode] : "бензин";
  const vol =
    car.engineVolume != null && car.engineVolume > 0
      ? `${car.engineVolume.toLocaleString("ru-RU")} л`
      : "";
  const trans = filtersTransmissionLabel(car);
  const bodyWord = (() => {
    if (code === "suv") return `внедорожник ${doors} дв.`;
    if (code === "sedan") return `седан ${doors} дв.`;
    if (code === "hatchback") return `хэтчбек ${doors} дв.`;
    if (code === "wagon") return `универсал ${doors} дв.`;
    if (code === "coupe") return `купе ${doors} дв.`;
    if (code === "cabriolet") return `кабриолет ${doors} дв.`;
    const b = bodyTypeLabel(car);
    return b !== "—" ? `${b} ${doors} дв.` : `${doors} дв.`;
  })();
  return [trans, vol, fuel, bodyWord].filter(Boolean).join(", ");
}

function listingSellerLine(car: Car): string {
  const name = car.sellerDisplayName?.trim();
  if (name) return name;
  if (car.sellerAccountType === "DEALERSHIP") return "Автосалон";
  if (car.sellerAccountType === "PERSON") return "Частный продавец";
  return "Продавец";
}

type FeatureApiRow = { id: number; name: string; category?: string | null };

/** Полный URL для путей вида /api/cars/1/photos/file.jpg */
function mediaUrl(apiPath: string): string {
  if (apiPath.startsWith("http://") || apiPath.startsWith("https://")) {
    return apiPath;
  }
  try {
    const u = new URL(API_BASE);
    const path = apiPath.startsWith("/") ? apiPath : `/${apiPath}`;
    return `${u.origin}${path}`;
  } catch {
    return apiPath;
  }
}

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return null as T;
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data?.message || data?.error || "Request failed");
  }
  return data as T;
}

async function apiAuth<T>(path: string, token: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  headers.set("Authorization", `Bearer ${token}`);
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return null as T;
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data?.message || data?.error || "Request failed");
  }
  return data as T;
}

function formatAdminAccountType(t: string): string {
  switch (t) {
    case "PERSON":
      return "Физ.лицо";
    case "DEALERSHIP":
      return "Юр.лицо";
    case "ADMIN":
      return "Админ";
    default:
      return t;
  }
}

function adminUserDisplayLabel(row: AdminUserRow): string {
  return (
    row.companyName?.trim() ||
    row.personName?.trim() ||
    row.displayName?.trim() ||
    "—"
  );
}

export function RelationsTab() {
  const [dealerships, setDealerships] = useState<Dealership[]>([]);
  const [selectedDealershipId, setSelectedDealershipId] = useState("");
  const [dealershipWithCars, setDealershipWithCars] =
    useState<DealershipWithCars | null>(null);
  const [category, setCategory] = useState("");
  const [carsByCategory, setCarsByCategory] = useState<Car[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api<Dealership[]>("/dealerships")
      .then((data) => setDealerships(data || []))
      .catch((e: Error) => setError(e.message));
  }, []);

  const hasCars = useMemo(
    () =>
      Array.isArray(dealershipWithCars?.cars) && dealershipWithCars.cars.length > 0,
    [dealershipWithCars]
  );

  return (
    <div>
      <section className="card">
        <h3>OneToMany: dealership - cars</h3>
        <div className="grid grid-2">
          <select
            value={selectedDealershipId}
            onChange={(e) => setSelectedDealershipId(e.target.value)}
          >
            <option value="">Select dealership</option>
            {dealerships.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
          <button
            onClick={async () => {
              if (!selectedDealershipId) return;
              try {
                const data = await api<DealershipWithCars>(
                  `/dealerships/${selectedDealershipId}/with-cars`
                );
                setDealershipWithCars(data);
              } catch (err) {
                setError((err as Error).message);
              }
            }}
          >
            Load cars
          </button>
        </div>
        {dealershipWithCars && (
          <div className="mt8">
            <strong>{dealershipWithCars.name}</strong>
            {!hasCars && <p>No cars in this dealership.</p>}
            {hasCars && (
              <table>
                <thead>
                  <tr>
                    <th>Brand</th>
                    <th>Model</th>
                    <th>Year</th>
                  </tr>
                </thead>
                <tbody>
                  {dealershipWithCars.cars.map((car) => (
                    <tr key={car.id}>
                      <td>{car.brand}</td>
                      <td>{car.model}</td>
                      <td>{car.year}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </section>

      <section className="card">
        <h3>ManyToMany: cars - features</h3>
        <form
          className="grid grid-2"
          onSubmit={async (e) => {
            e.preventDefault();
            if (!category.trim()) {
              setCarsByCategory([]);
              return;
            }
            try {
              const data = await api<Car[]>(
                `/cars/search/jpql?category=${encodeURIComponent(category.trim())}`
              );
              setCarsByCategory(data || []);
            } catch (err) {
              setError((err as Error).message);
            }
          }}
        >
          <input
            placeholder="Feature category (e.g. Комфорт)"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />
          <button type="submit">Find cars</button>
        </form>
        {carsByCategory.length > 0 && (
          <table className="mt8">
            <thead>
              <tr>
                <th>Brand</th>
                <th>Model</th>
                <th>Year</th>
              </tr>
            </thead>
            <tbody>
              {carsByCategory.map((car) => (
                <tr key={car.id}>
                  <td>{car.brand}</td>
                  <td>{car.model}</td>
                  <td>{car.year}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {error && <p className="message error">{error}</p>}
      </section>
    </div>
  );
}

type Tab = "cars" | "relations";
type UserMode = "buyer" | "seller";
type AccountKind = "person" | "dealership" | "admin";
type AuthSession = {
  token: string;
  username: string;
  accountType: AccountKind;
  displayName?: string;
  personName?: string;
  companyName?: string;
  address?: string;
};

type AuthApiBody = {
  token: string | null;
  username: string;
  accountType: "PERSON" | "DEALERSHIP" | "ADMIN";
  displayName?: string | null;
  personName?: string | null;
  companyName?: string | null;
  address?: string | null;
};

type AdminUserRow = {
  id: number;
  username: string;
  accountType: string;
  displayName?: string | null;
  personName?: string | null;
  companyName?: string | null;
  phone?: string | null;
  address?: string | null;
};

function sessionFromAuthBody(body: AuthApiBody, tokenFallback: string): AuthSession {
  const token = body.token && body.token.length > 0 ? body.token : tokenFallback;
  let accountType: AccountKind = "person";
  if (body.accountType === "DEALERSHIP") accountType = "dealership";
  else if (body.accountType === "ADMIN") accountType = "admin";
  return {
    token,
    username: body.username,
    accountType,
    displayName: body.displayName ?? undefined,
    personName: body.personName ?? undefined,
    companyName: body.companyName ?? undefined,
    address: body.address ?? undefined,
  };
}
type HeroModelNavigate = { nonce: number; brand: string; model: string };

type CarsTabProps = {
  onCarsLoaded: (cars: Car[]) => void;
  onCarsLoadStatus?: (status: "loading" | "ok" | "error") => void;
  selectedBrandFromHero?: string;
  resetToListSignal?: number;
  heroModelNavigate?: HeroModelNavigate | null;
  /** Режим покупателя: открыта карточка объявления (чтобы скрыть hero на уровне App). */
  onBuyerCarDetailOpen?: (open: boolean) => void;
  mode: UserMode;
  currentUser: AuthSession | null;
  favoriteCarIds: Set<number>;
  onFavoriteToggle: (carId: number) => void;
  /** Открыть карточку объявления у покупателя (после выбора в избранном). */
  buyerOpenCarRequest: { id: number; nonce: number } | null;
  onBuyerOpenCarRequestHandled: () => void;
};

type CarDetailsProps = {
  car: Car;
  onBack: () => void;
  isFavorite: boolean;
  onFavoriteToggle: () => void;
  viewerPhone?: string | null;
};

type PickerOption = {
  value: string;
  label: string;
};

const LISTING_CURRENCY_OPTIONS: PickerOption[] = [
  { value: "USD", label: "USD" },
  { value: "BYN", label: "BYN" },
];

type ListingSortKey =
  | "price_asc"
  | "price_desc"
  | "date_desc"
  | "date_asc"
  | "mileage_asc"
  | "year_desc"
  | "year_asc";

const LISTING_SORT_OPTIONS: { value: ListingSortKey; label: string }[] = [
  { value: "price_asc", label: "Дешёвые" },
  { value: "price_desc", label: "Дорогие" },
  { value: "date_desc", label: "Новые объявления" },
  { value: "date_asc", label: "Старые объявления" },
  { value: "mileage_asc", label: "С наименьшим пробегом" },
  { value: "year_desc", label: "Новые по году" },
  { value: "year_asc", label: "Старые по году" },
];

/** Верхняя граница цены в выбранной валюте (форма и API). */
const MAX_CAR_PRICE = 1_000_000;
/** Синхронно с бэкендом (CarModelYear). */
const MIN_CAR_MODEL_YEAR = 1886;
const MAX_CAR_MODEL_YEAR = 2026;

/** Максимум позиций в одном пакете для автосалона (как на бэкенде). */
const MAX_DEALERSHIP_BULK_ROWS = 30;

const BUYER_FILTERS_INITIAL = {
  brand: "",
  brandMode: "include" as "include" | "exclude",
  model: "",
  yearFrom: "",
  yearTo: "",
  priceFrom: "",
  priceTo: "",
  priceCurrency: "USD" as ListingCurrency,
  transmission: "",
  bodyType: "",
  engineType: "",
  driveType: "",
  powerFrom: "",
  powerTo: "",
  consumptionFrom: "",
  consumptionTo: "",
  rangeFrom: "",
  rangeTo: "",
  bodyColor: "",
  interiorColor: "",
  interiorMaterial: "",
  seats: "",
  selectedFeatures: [] as string[],
};

const DEFAULT_INTERIOR_MATERIALS = [
  "Кожа",
  "Ткань",
  "Алькантара",
  "Велюр",
  "Комбинированный",
];

/** Варианты цвета кузова при подаче объявления (значение = подпись в БД). */
const SELLER_BODY_COLOR_OPTIONS: PickerOption[] = [
  { value: "Белый", label: "Белый" },
  { value: "Чёрный", label: "Чёрный" },
  { value: "Серый", label: "Серый" },
  { value: "Серебристый", label: "Серебристый" },
  { value: "Тёмно-серый", label: "Тёмно-серый" },
  { value: "Графит", label: "Графит" },
  { value: "Красный", label: "Красный" },
  { value: "Бордовый", label: "Бордовый" },
  { value: "Синий", label: "Синий" },
  { value: "Голубой", label: "Голубой" },
  { value: "Зелёный", label: "Зелёный" },
  { value: "Жёлтый", label: "Жёлтый" },
  { value: "Оранжевый", label: "Оранжевый" },
  { value: "Коричневый", label: "Коричневый" },
  { value: "Бежевый", label: "Бежевый" },
  { value: "Золотистый", label: "Золотистый" },
  { value: "Фиолетовый", label: "Фиолетовый" },
  { value: "Чёрный металлик", label: "Чёрный металлик" },
  { value: "Серый металлик", label: "Серый металлик" },
  { value: "Серебристый металлик", label: "Серебристый металлик" },
  { value: "Белый перламутр", label: "Белый перламутр" },
  { value: "Другой", label: "Другой" },
];

/** Варианты цвета салона при подаче объявления. */
const SELLER_INTERIOR_COLOR_OPTIONS: PickerOption[] = [
  { value: "Чёрный", label: "Чёрный" },
  { value: "Серый", label: "Серый" },
  { value: "Светло-серый", label: "Светло-серый" },
  { value: "Тёмно-серый", label: "Тёмно-серый" },
  { value: "Бежевый", label: "Бежевый" },
  { value: "Коричневый", label: "Коричневый" },
  { value: "Тёмно-коричневый", label: "Тёмно-коричневый" },
  { value: "Кремовый", label: "Кремовый" },
  { value: "Белый", label: "Белый" },
  { value: "Красный", label: "Красный" },
  { value: "Синий", label: "Синий" },
  { value: "Бордовый", label: "Бордовый" },
  { value: "Оранжевый", label: "Оранжевый" },
  { value: "Комбинированный", label: "Комбинированный" },
  { value: "Другой", label: "Другой" },
];

/** Поля одной позиции в пакетном добавлении (те же правила, что у одиночной формы). */
type DealershipBulkRowFields = {
  brand: string;
  model: string;
  year: string;
  city: string;
  color: string;
  interiorColor: string;
  interiorMaterial: string;
  engineVolume: string;
  mileage: string;
  powerHp: string;
  fuelConsumptionCity: string;
  fuelConsumptionHighway: string;
  fuelConsumptionMixed: string;
  seats: string;
  price: string;
  transmission: string;
  bodyType: string;
  engineType: string;
  driveType: string;
  /** Только ADMIN: user_accounts.id владельца объявления */
  ownerUserId: string;
};

type DealershipBulkRow = { id: number } & DealershipBulkRowFields;

type CarCreateApiPayload = {
  brand: string;
  model: string;
  year: number;
  color: string;
  interiorColor: string;
  interiorMaterial: string;
  engineVolume: number;
  mileage: number;
  powerHp: number;
  fuelConsumptionCity: number;
  fuelConsumptionHighway: number;
  fuelConsumptionMixed: number;
  seatCount: number;
  city: string;
  transmission: string;
  bodyType: string;
  engineType: string;
  driveType: string;
  price: number;
  priceCurrency: ListingCurrency;
  featureIds: number[];
  ownerUserId?: number;
};

function emptyDealershipBulkRowFields(): DealershipBulkRowFields {
  return {
    brand: "",
    model: "",
    year: "",
    city: "",
    color: "",
    interiorColor: "",
    interiorMaterial: "",
    engineVolume: "",
    mileage: "",
    powerHp: "",
    fuelConsumptionCity: "",
    fuelConsumptionHighway: "",
    fuelConsumptionMixed: "",
    seats: "",
    price: "",
    transmission: "",
    bodyType: "",
    engineType: "",
    driveType: "",
    ownerUserId: "",
  };
}

function buildCarPayloadFromSellerFields(
  row: DealershipBulkRowFields,
  options?: { priceCurrency?: ListingCurrency; featureIds?: number[] }
): { ok: true; payload: CarCreateApiPayload } | { ok: false; message: string } {
  const yearNum = Number(row.year);
  const priceNum = Number(row.price);
  const engineNum = row.engineVolume.trim() ? Number(row.engineVolume.replace(",", ".")) : NaN;
  const mileageNum = row.mileage.trim() ? Number(row.mileage) : NaN;
  const powerNum = row.powerHp.trim() ? Number(row.powerHp) : NaN;
  const fcCity = row.fuelConsumptionCity.trim()
    ? Number(row.fuelConsumptionCity.replace(",", "."))
    : NaN;
  const fcHwy = row.fuelConsumptionHighway.trim()
    ? Number(row.fuelConsumptionHighway.replace(",", "."))
    : NaN;
  const fcMix = row.fuelConsumptionMixed.trim()
    ? Number(row.fuelConsumptionMixed.replace(",", "."))
    : NaN;
  const seatsNum = row.seats.trim() ? Number(row.seats) : NaN;

  const missing: string[] = [];
  if (!row.brand.trim()) missing.push("марку");
  if (!row.model.trim()) missing.push("модель");
  if (!row.year.trim() || !Number.isFinite(yearNum)) missing.push("год");
  else if (yearNum < MIN_CAR_MODEL_YEAR || yearNum > MAX_CAR_MODEL_YEAR) {
    return {
      ok: false,
      message: `Год выпуска должен быть от ${MIN_CAR_MODEL_YEAR} до ${MAX_CAR_MODEL_YEAR}.`,
    };
  }
  const priceCur: ListingCurrency = options?.priceCurrency === "BYN" ? "BYN" : "USD";
  if (!row.price.trim() || !Number.isFinite(priceNum) || priceNum <= 0) missing.push("цену");
  else if (priceNum > MAX_CAR_PRICE) {
    return {
      ok: false,
      message: `Цена не может быть больше ${MAX_CAR_PRICE.toLocaleString("ru-RU")} ${priceCur}.`,
    };
  }
  if (!row.city.trim()) missing.push("город");
  if (!row.color.trim()) missing.push("цвет кузова");
  if (!row.interiorColor.trim()) missing.push("цвет салона");
  if (!row.interiorMaterial.trim()) missing.push("материал салона");
  if (!row.engineVolume.trim() || !Number.isFinite(engineNum) || engineNum <= 0) {
    missing.push("объём двигателя");
  }
  if (!row.mileage.trim() || !Number.isFinite(mileageNum) || mileageNum < 0) {
    missing.push("пробег");
  }
  if (!row.powerHp.trim() || !Number.isFinite(powerNum) || powerNum < 1) {
    missing.push("мощность");
  }
  if (!row.fuelConsumptionCity.trim() || !Number.isFinite(fcCity) || fcCity <= 0) {
    missing.push("расход по городу");
  }
  if (!row.fuelConsumptionHighway.trim() || !Number.isFinite(fcHwy) || fcHwy <= 0) {
    missing.push("расход по трассе");
  }
  if (!row.fuelConsumptionMixed.trim() || !Number.isFinite(fcMix) || fcMix <= 0) {
    missing.push("смешанный расход");
  }
  if (!row.seats.trim() || !Number.isFinite(seatsNum) || seatsNum < 1 || seatsNum > 9) {
    missing.push("количество мест");
  }
  if (!row.transmission.trim()) missing.push("коробку передач");
  if (!row.bodyType.trim()) missing.push("тип кузова");
  if (!row.engineType.trim()) missing.push("тип двигателя");
  if (!row.driveType.trim()) missing.push("тип привода");
  if (missing.length) {
    return { ok: false, message: "Заполните обязательные поля" };
  }

  const payload: CarCreateApiPayload = {
    brand: row.brand.trim(),
    model: row.model.trim(),
    year: yearNum,
    color: row.color.trim(),
    interiorColor: row.interiorColor.trim(),
    interiorMaterial: row.interiorMaterial.trim(),
    engineVolume: engineNum,
    mileage: Math.trunc(mileageNum),
    powerHp: Math.trunc(powerNum),
    fuelConsumptionCity: fcCity,
    fuelConsumptionHighway: fcHwy,
    fuelConsumptionMixed: fcMix,
    seatCount: Math.trunc(seatsNum),
    city: row.city.trim(),
    transmission: row.transmission.trim(),
    bodyType: row.bodyType.trim(),
    engineType: row.engineType.trim(),
    driveType: row.driveType.trim(),
    price: priceNum,
    priceCurrency: priceCur,
    featureIds: options?.featureIds?.length ? [...options.featureIds] : [],
  };
  const ou = row.ownerUserId?.trim();
  if (ou) {
    const oid = Number(ou);
    if (Number.isFinite(oid) && oid > 0) payload.ownerUserId = oid;
  }
  return { ok: true, payload };
}

async function copyTextToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    try {
      const ta = document.createElement("textarea");
      ta.value = text;
      ta.setAttribute("readonly", "");
      ta.style.position = "fixed";
      ta.style.left = "-9999px";
      document.body.appendChild(ta);
      ta.select();
      const ok = document.execCommand("copy");
      document.body.removeChild(ta);
      return ok;
    } catch {
      return false;
    }
  }
}

function formatBelarusPhoneForContact(raw: string): string {
  let digits = raw.replace(/\D/g, "");
  if (digits.startsWith("375")) {
    digits = digits.slice(3);
  }
  digits = digits.slice(0, 9);

  let formatted = "+375";
  if (digits.length === 0) return formatted;

  formatted += ` (${digits.slice(0, Math.min(2, digits.length))}`;
  if (digits.length >= 2) formatted += ")";
  if (digits.length > 2) formatted += ` ${digits.slice(2, Math.min(5, digits.length))}`;
  if (digits.length > 5) formatted += `-${digits.slice(5, Math.min(7, digits.length))}`;
  if (digits.length > 7) formatted += `-${digits.slice(7, 9)}`;
  return formatted;
}

function CarDetailsPage({ car, onBack, isFavorite, onFavoriteToggle, viewerPhone }: CarDetailsProps) {
  const [activePhoto, setActivePhoto] = useState(0);
  const [callSellerNotice, setCallSellerNotice] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [contactPhoneNotice, setContactPhoneNotice] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [contactPhoneDraft, setContactPhoneDraft] = useState(() => {
    try {
      return formatBelarusPhoneForContact(localStorage.getItem("autosalon_contact_phone_draft") || "");
    } catch {
      return "+375";
    }
  });
  const callSellerNoticeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (callSellerNoticeTimerRef.current != null) clearTimeout(callSellerNoticeTimerRef.current);
    };
  }, []);

  const showCallSellerNotice = useCallback((kind: "ok" | "err", text: string) => {
    if (callSellerNoticeTimerRef.current != null) clearTimeout(callSellerNoticeTimerRef.current);
    setCallSellerNotice({ kind, text });
    callSellerNoticeTimerRef.current = setTimeout(() => {
      setCallSellerNotice(null);
      callSellerNoticeTimerRef.current = null;
    }, 4000);
  }, []);

  const handleCallSeller = useCallback(async () => {
    const phone = car.sellerPhone?.trim();
    if (!phone) {
      showCallSellerNotice("err", "У продавца не указан телефон в профиле.");
      return;
    }
    const ok = await copyTextToClipboard(phone);
    if (ok) {
      showCallSellerNotice("ok", `Номер ${phone} скопирован в буфер обмена.`);
    } else {
      showCallSellerNotice("err", "Не удалось скопировать номер. Скопируйте вручную: " + phone);
    }
  }, [car.sellerPhone, showCallSellerNotice]);
  const galleryUrls = useMemo(() => {
    if (car.photos && car.photos.length > 0) {
      return car.photos.map((p) => mediaUrl(p.url));
    }
    const seeds = [
      `${car.brand}-${car.model}-1`,
      `${car.brand}-${car.model}-2`,
      `${car.brand}-${car.model}-3`,
      `${car.brand}-${car.model}-4`,
      `${car.brand}-${car.model}-5`,
    ];
    return seeds.map(
      (seed) => `https://picsum.photos/seed/${encodeURIComponent(seed)}/1200/760`
    );
  }, [car.brand, car.model, car.photos]);

  useEffect(() => {
    setActivePhoto(0);
  }, [car.id, galleryUrls.length]);

  useEffect(() => {
    if (callSellerNoticeTimerRef.current != null) {
      clearTimeout(callSellerNoticeTimerRef.current);
      callSellerNoticeTimerRef.current = null;
    }
    setCallSellerNotice(null);
  }, [car.id]);

  const photos = galleryUrls;
  const canSlidePhotos = photos.length > 1;
  const showPrevPhoto = () => {
    setActivePhoto((prev) => (prev - 1 + photos.length) % photos.length);
  };
  const showNextPhoto = () => {
    setActivePhoto((prev) => (prev + 1) % photos.length);
  };

  useEffect(() => {
    const phone = viewerPhone?.trim();
    if (!phone) return;
    setContactPhoneDraft(formatBelarusPhoneForContact(phone));
  }, [viewerPhone]);

  const confirmContactPhone = () => {
    const value = contactPhoneDraft.trim();
    const phoneMaskPattern = /^\+375 \(\d{2}\) \d{3}-\d{2}-\d{2}$/;
    if (!phoneMaskPattern.test(value)) {
      setContactPhoneNotice({ kind: "err", text: "Введите номер в формате +375 (XX) XXX-XX-XX." });
      return;
    }
    try {
      localStorage.setItem("autosalon_contact_phone_draft", value);
      setContactPhoneNotice({ kind: "ok", text: "Номер сохранён для связи." });
    } catch {
      setContactPhoneNotice({ kind: "err", text: "Не удалось сохранить номер. Попробуйте снова." });
    }
  };

  return (
    <section className="car-details card">
      <button type="button" className="secondary details-back" onClick={onBack}>
        ← Назад к списку
      </button>

      <div className="details-title-row">
        <h2 className="details-title">
          Продажа {car.brand} {car.model}, {car.year} г.
          {car.city?.trim() ? ` в ${car.city.trim()}` : ""}
        </h2>
        <button
          type="button"
          className={`details-fav-btn ${isFavorite ? "details-fav-btn--on" : ""}`}
          aria-label={isFavorite ? "Убрать из избранного" : "Добавить в избранное"}
          aria-pressed={isFavorite}
          onClick={onFavoriteToggle}
        >
          {isFavorite ? "♥" : "♡"}
        </button>
      </div>
      <p className="details-techline">{detailsHeaderSpecLine(car)}</p>
      <p className="details-subtitle">{car.color}, в наличии</p>
      <p className="details-published">Опубликовано: {formatPublishedRelative(car.publishedAt)}</p>
      {(car.sellerAccountType === "DEALERSHIP" || car.sellerAccountType === "PERSON") && (
        <p className="details-seller-line">
          {car.sellerAccountType === "DEALERSHIP" ? (
            <span className="seller-badge seller-badge--dealer">Продавец: автосалон</span>
          ) : (
            <span className="seller-badge seller-badge--person">Продавец: частное лицо</span>
          )}
        </p>
      )}

      <div className="details-layout">
        <div className="details-gallery">
          <div className="details-main-image-wrap">
            <img className="details-main-image" src={photos[activePhoto]} alt={`${car.brand} ${car.model}`} />
            {canSlidePhotos && (
              <>
                <button
                  type="button"
                  className="details-gallery-arrow details-gallery-arrow--prev"
                  aria-label="Предыдущее фото"
                  onClick={showPrevPhoto}
                >
                  ‹
                </button>
                <button
                  type="button"
                  className="details-gallery-arrow details-gallery-arrow--next"
                  aria-label="Следующее фото"
                  onClick={showNextPhoto}
                >
                  ›
                </button>
              </>
            )}
          </div>
          <div className="details-thumbs">
            {photos.map((src, idx) => (
              <button
                key={src}
                type="button"
                className={`details-thumb ${activePhoto === idx ? "active" : ""}`}
                onClick={() => setActivePhoto(idx)}
              >
                <img src={src} alt={`${car.brand} ${car.model} фото ${idx + 1}`} />
              </button>
            ))}
          </div>
        </div>

        <aside className="details-side">
          <div className="details-price">
            <PriceDualBlock price={car.price} currencyCode={car.priceCurrency} />
          </div>
          <p className="details-meta">
            {car.year} г., {filtersTransmissionLabel(car)}, {engineLabel(car)}, пробег {mileageLabel(car)}
            {car.powerHp != null && car.powerHp > 0
              ? `, ${car.powerHp.toLocaleString("ru-RU")} л.с.`
              : ""}
          </p>
          <p className="details-meta">
            Цвет: {car.color}, кузов: {bodyTypeLabel(car)}, салон: {car.interiorColor?.trim() || "—"}
            {car.interiorMaterial?.trim() ? ` (${car.interiorMaterial.trim()})` : ""}, привод {driveLabel(car)}
            {car.seatCount != null && car.seatCount > 0 ? `, ${car.seatCount} мест` : ""}
          </p>
          {(car.fuelConsumptionCity != null ||
            car.fuelConsumptionHighway != null ||
            car.fuelConsumptionMixed != null) && (
            <p className="details-meta">
              Расход: город{" "}
              {car.fuelConsumptionCity != null ? `${car.fuelConsumptionCity} л/100 км` : "—"}, трасса{" "}
              {car.fuelConsumptionHighway != null ? `${car.fuelConsumptionHighway} л/100 км` : "—"}, смешанный{" "}
              {car.fuelConsumptionMixed != null ? `${car.fuelConsumptionMixed} л/100 км` : "—"}
            </p>
          )}
          {callSellerNotice && (
            <p
              className={callSellerNotice.kind === "ok" ? "message success details-call-notice" : "message error details-call-notice"}
              role="status"
            >
              {callSellerNotice.text}
            </p>
          )}
          <button type="button" className="show-results-btn details-primary-btn" onClick={() => void handleCallSeller()}>
            Позвонить продавцу
          </button>
          <div className="details-contact-box">
            <p className="details-contact-title">Оставить свой номер телефона для связи</p>
            <input
              type="tel"
              inputMode="numeric"
              maxLength={19}
              placeholder="+375 (XX) XXX-XX-XX"
              value={contactPhoneDraft}
              onFocus={() => {
                if (!contactPhoneDraft.trim()) setContactPhoneDraft("+375");
              }}
              onChange={(e) => setContactPhoneDraft(formatBelarusPhoneForContact(e.target.value))}
            />
            <button type="button" className="secondary details-contact-submit" onClick={confirmContactPhone}>
              Оставить номер
            </button>
            {contactPhoneNotice && (
              <p className={contactPhoneNotice.kind === "ok" ? "message success" : "message error"} role="status">
                {contactPhoneNotice.text}
              </p>
            )}
          </div>
        </aside>
      </div>

      <div className="details-features">
        <h3>Комплектация</h3>
        {car.featureNames && car.featureNames.length > 0 ? (
          <div className="details-feature-section">
            <div className="details-chip-wrap">
              {car.featureNames.map((name, idx) => (
                <span key={`${name}-${idx}`} className="details-chip">
                  {name}
                </span>
              ))}
            </div>
          </div>
        ) : (
          <p className="details-meta details-features-empty">
            В объявлении не указаны опции комплектации.
          </p>
        )}
      </div>
    </section>
  );
}

const TRANSMISSION_LABELS: Record<string, string> = {
  auto: "автомат",
  manual: "механика",
  robot: "робот",
};
const ENGINE_LABELS: Record<string, string> = {
  petrol: "бензин",
  diesel: "дизель",
  electric: "электро",
};
const DRIVE_LABELS: Record<string, string> = {
  fwd: "передний",
  rwd: "задний",
  awd: "полный",
};
const BODY_LABELS: Record<string, string> = {
  sedan: "седан",
  suv: "SUV",
  hatchback: "хэтчбек",
  wagon: "универсал",
  coupe: "купе",
  cabriolet: "кабриолет",
};

function filtersTransmissionLabel(car: Car): string {
  const code = car.transmission?.trim().toLowerCase();
  if (code && TRANSMISSION_LABELS[code]) return TRANSMISSION_LABELS[code];
  return car.model.toLowerCase().includes("m") ? "механика" : "автомат";
}

function engineLabel(car: Car): string {
  const fuelCode = car.engineType?.trim().toLowerCase();
  const fuel =
    fuelCode && ENGINE_LABELS[fuelCode]
      ? ENGINE_LABELS[fuelCode]
      : car.price > 120000
        ? "бензин"
        : car.price > 70000
          ? "дизель"
          : "бензин";
  if (car.engineVolume && car.engineVolume > 0) {
    return `${car.engineVolume.toLocaleString("ru-RU")} л, ${fuel}`;
  }
  if (car.price > 120000) return `3.0 л, ${fuel}`;
  if (car.price > 70000) return `2.0 л, ${fuel}`;
  return `1.8 л, ${fuel}`;
}

function mileageLabel(car: Car): string {
  if (car.mileage !== null && car.mileage !== undefined && car.mileage >= 0) {
    return `${car.mileage.toLocaleString("ru-RU")} км`;
  }
  const approx = Math.max(15000, 220000 - (car.year - 2018) * 18000);
  return `${approx.toLocaleString("ru-RU")} км`;
}

function driveLabel(car: Car): string {
  const code = car.driveType?.trim().toLowerCase();
  if (code && DRIVE_LABELS[code]) return DRIVE_LABELS[code];
  if (["Audi", "BMW", "Mercedes", "Porsche", "Volvo", "Lexus", "Toyota"].includes(car.brand)) {
    return "полный";
  }
  return "передний";
}

function bodyTypeLabel(car: Car): string {
  const code = car.bodyType?.trim().toLowerCase();
  if (code && BODY_LABELS[code]) return BODY_LABELS[code];
  return car.bodyType?.trim() || "—";
}

function detailsHeaderSpecLine(car: Car): string {
  const transmissionCode = car.transmission?.trim().toLowerCase();
  const transmissionShort =
    transmissionCode === "manual"
      ? "MT"
      : transmissionCode === "robot"
        ? "AMT"
        : transmissionCode === "auto"
          ? "AT"
          : "AT";

  const driveCode = car.driveType?.trim().toLowerCase();
  const driveShort =
    driveCode === "awd" ? "AWD" : driveCode === "rwd" ? "RWD" : driveCode === "fwd" ? "FWD" : "FWD";

  const volume =
    car.engineVolume != null && car.engineVolume > 0
      ? car.engineVolume.toLocaleString("ru-RU", { minimumFractionDigits: 1, maximumFractionDigits: 1 })
      : "2,0";

  const power =
    car.powerHp != null && car.powerHp > 0 ? car.powerHp.toLocaleString("ru-RU") : "—";

  return `${volume} ${transmissionShort} ${driveShort} (${power} л.с.)`;
}

function normalizeYearInput(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, 4);
  if (digitsOnly.length === 4) {
    const n = Number(digitsOnly);
    if (n > MAX_CAR_MODEL_YEAR) return String(MAX_CAR_MODEL_YEAR);
    if (n < MIN_CAR_MODEL_YEAR) return String(MIN_CAR_MODEL_YEAR);
  }
  return digitsOnly;
}

function normalizeCappedNumberInput(
  value: string,
  max: number,
  { allowDecimal = false }: { allowDecimal?: boolean } = {}
): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  const normalized = allowDecimal ? trimmed.replace(",", ".") : trimmed;
  const parsed = Number(normalized);
  if (!Number.isFinite(parsed)) return "";
  const capped = Math.min(parsed, max);
  return allowDecimal ? String(capped) : String(Math.trunc(capped));
}

type SimpleFilterPickerProps = {
  value: string;
  placeholder: string;
  options: PickerOption[];
  onChange: (value: string) => void;
  /** Узкий прямоугольник (валюта и т.п.) в стиле остальных brand-picker. */
  compact?: boolean;
};

function SimpleFilterPicker({
  value,
  placeholder,
  options,
  onChange,
  compact = false,
}: SimpleFilterPickerProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onPointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (rootRef.current && !rootRef.current.contains(target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onPointerDown);
    return () => document.removeEventListener("mousedown", onPointerDown);
  }, []);

  const selectedLabel = options.find((option) => option.value === value)?.label || placeholder;

  return (
    <div
      ref={rootRef}
      className={`brand-picker${compact ? " brand-picker--compact" : ""} ${open ? "is-open" : ""}`}
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        type="button"
        className="brand-picker-trigger"
        onClick={() => setOpen((prev) => !prev)}
        onFocus={() => setOpen(true)}
      >
        <span className="brand-picker-trigger-label">{selectedLabel}</span>
        <span className="brand-picker-chevron" aria-hidden>
          ▾
        </span>
      </button>
      {open && (
        <div className="brand-picker-panel">
          <ul className="brand-dropdown" role="listbox">
            {options.map((option) => (
              <li key={option.value || "any"}>
                <button
                  type="button"
                  className="brand-dropdown-item"
                  onClick={() => {
                    onChange(option.value);
                    setOpen(false);
                  }}
                >
                  {option.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function ListingSortPicker({
  value,
  onChange,
}: {
  value: ListingSortKey;
  onChange: (sort: ListingSortKey) => void;
}) {
  const sortLabelId = useId();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onPointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (rootRef.current && !rootRef.current.contains(target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onPointerDown);
    return () => document.removeEventListener("mousedown", onPointerDown);
  }, []);

  const selectedLabel =
    LISTING_SORT_OPTIONS.find((o) => o.value === value)?.label ?? LISTING_SORT_OPTIONS[0].label;

  return (
    <div className="listing-sort-picker" ref={rootRef}>
      <span className="listing-sort-label" id={sortLabelId}>
        Сортировка
      </span>
      <button
        type="button"
        className="listing-sort-trigger"
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-labelledby={sortLabelId}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="listing-sort-trigger-label">{selectedLabel}</span>
        <span className="listing-sort-chevron" aria-hidden>
          ▾
        </span>
      </button>
      {open && (
        <ul className="listing-sort-menu" role="listbox" aria-labelledby={sortLabelId}>
          {LISTING_SORT_OPTIONS.map((opt) => (
            <li key={opt.value} role="none">
              <button
                type="button"
                role="option"
                aria-selected={value === opt.value}
                className={`listing-sort-item${value === opt.value ? " listing-sort-item--active" : ""}`}
                onClick={() => {
                  onChange(opt.value);
                  setOpen(false);
                }}
              >
                {opt.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function CarsTabWithDesign({
  onCarsLoaded,
  onCarsLoadStatus,
  selectedBrandFromHero,
  resetToListSignal,
  heroModelNavigate,
  onBuyerCarDetailOpen,
  mode,
  currentUser,
  favoriteCarIds,
  onFavoriteToggle,
  buyerOpenCarRequest,
  onBuyerOpenCarRequestHandled,
}: CarsTabProps) {
  const emptyForm = {
    brand: "",
    model: "",
    year: "",
    city: "",
    color: "",
    interiorColor: "",
    interiorMaterial: "",
    engineVolume: "",
    mileage: "",
    powerHp: "",
    fuelConsumptionCity: "",
    fuelConsumptionHighway: "",
    fuelConsumptionMixed: "",
    seats: "",
    price: "",
    priceCurrency: "USD" as ListingCurrency,
    ownerUserId: "",
  };
  const [cars, setCars] = useState<Car[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  /** После сохранения нового объявления — id для загрузки фото, пока не сбросили форму или не открыли другое объявление. */
  const [photoUploadCarId, setPhotoUploadCarId] = useState<number | null>(null);
  const [photoBusy, setPhotoBusy] = useState(false);
  const [createPhotoFiles, setCreatePhotoFiles] = useState<File[]>([]);
  /** Сообщение прямо в блоке фото (глобальное message уезжало вниз страницы). */
  const [photoBanner, setPhotoBanner] = useState<{ kind: "ok" | "err"; text: string } | null>(null);

  /** Только редактируемое объявление или только что сохранённое — без fallback на «первую в списке». */
  const photoCarIdResolved = useMemo(() => {
    if (editingId != null && cars.some((c) => c.id === editingId)) {
      return editingId;
    }
    if (photoUploadCarId != null && cars.some((c) => c.id === photoUploadCarId)) {
      return photoUploadCarId;
    }
    return null;
  }, [cars, editingId, photoUploadCarId]);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [filters, setFilters] = useState(() => ({ ...BUYER_FILTERS_INITIAL }));
  const [appliedFilters, setAppliedFilters] = useState(() => ({ ...BUYER_FILTERS_INITIAL }));
  const [buyerSort, setBuyerSort] = useState<ListingSortKey>("date_desc");
  const [listingsPage, setListingsPage] = useState(1);
  const [paginationScrollIntent, setPaginationScrollIntent] = useState<"top" | "bottom" | null>(null);
  const LISTINGS_PER_PAGE = 10;
  const resetBuyerFilters = () => {
    const reset = { ...BUYER_FILTERS_INITIAL };
    setFilters(reset);
    setAppliedFilters(reset);
    setBuyerSort("date_desc");
    setListingsPage(1);
  };
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [showSellerAdvancedFilters, setShowSellerAdvancedFilters] = useState(false);
  const [openedSellerSection, setOpenedSellerSection] = useState<string | null>(null);
  const [sellerExtras, setSellerExtras] = useState(() => ({ ...BUYER_FILTERS_INITIAL }));
  const [brandPickerBuyerOpen, setBrandPickerBuyerOpen] = useState(false);
  const [modelPickerBuyerOpen, setModelPickerBuyerOpen] = useState(false);
  const [brandPanelSearch, setBrandPanelSearch] = useState("");
  const [modelPanelSearch, setModelPanelSearch] = useState("");
  const bulkRowIdRef = useRef(0);
  const [bulkRows, setBulkRows] = useState<DealershipBulkRow[]>(() => [
    { id: ++bulkRowIdRef.current, ...emptyDealershipBulkRowFields() },
  ]);
  const [bulkMessage, setBulkMessage] = useState("");
  const [bulkError, setBulkError] = useState("");
  const [bulkBusy, setBulkBusy] = useState(false);
  const [bulkPriceCurrency, setBulkPriceCurrency] = useState<ListingCurrency>("USD");
  /** Марки на экране = из ответа API, не «напрямую из БД» */
  const [carsLoadStatus, setCarsLoadStatus] = useState<"loading" | "ok" | "error">("loading");
  const [openedSection, setOpenedSection] = useState<string | null>(null);
  const [selectedCar, setSelectedCar] = useState<Car | null>(null);
  const buyerBrandPickerRef = useRef<HTMLDivElement | null>(null);
  const buyerModelPickerRef = useRef<HTMLDivElement | null>(null);
  const listingsTopRef = useRef<HTMLDivElement | null>(null);
  const listingsBottomRef = useRef<HTMLDivElement | null>(null);
  const lastHeroModelNavNonceRef = useRef<number | null>(null);
  const sellerPhotoInputRef = useRef<HTMLInputElement | null>(null);
  const createPhotoInputRef = useRef<HTMLInputElement | null>(null);
  const photoUploadLockRef = useRef(false);
  const [featureCatalog, setFeatureCatalog] = useState<FeatureApiRow[]>([]);

  const toggleListingFavorite = useCallback(
    (carId: number, e: ReactMouseEvent<HTMLButtonElement>) => {
      e.stopPropagation();
      onFavoriteToggle(carId);
    },
    [onFavoriteToggle]
  );

  useEffect(() => {
    void api<FeatureApiRow[]>("/features")
      .then((list) => setFeatureCatalog(list || []))
      .catch(() => setFeatureCatalog([]));
  }, []);

  const featureSections = useMemo(() => {
    const byCat = new Map<string, Map<string, string>>();
    for (const f of featureCatalog) {
      const c = (f.category && f.category.trim()) || "Прочее";
      const rawName = f.name?.trim();
      if (!rawName) continue;
      const normalizedCategory = c.toLocaleLowerCase("ru");
      const normalizedName = rawName.toLocaleLowerCase("ru");
      const canonicalName =
        normalizedName === "обогрев сидений" ? "подогрев сидений" : normalizedName;
      if (normalizedCategory === "комфорт" && canonicalName === "кожаный салон") {
        continue;
      }
      let namesByKey = byCat.get(c);
      if (!namesByKey) {
        namesByKey = new Map<string, string>();
        byCat.set(c, namesByKey);
      }
      if (!namesByKey.has(canonicalName)) {
        const displayName = canonicalName === "подогрев сидений" ? "Подогрев сидений" : rawName;
        namesByKey.set(canonicalName, displayName);
      }
    }
    return [...byCat.entries()]
      .sort((a, b) => a[0].localeCompare(b[0], "ru"))
      .map(([title, namesByKey]) => ({
        title,
        items: [...namesByKey.values()].sort((x, y) => x.localeCompare(y, "ru", { sensitivity: "base" })),
      }));
  }, [featureCatalog]);

  const loadCars = async () => {
    setCarsLoadStatus("loading");
    onCarsLoadStatus?.("loading");
    setError("");
    try {
      const data =
        mode === "seller" && currentUser
          ? currentUser.accountType === "admin"
            ? await api<Car[]>("/cars")
            : await api<Car[]>("/cars/mine", {
                headers: { Authorization: `Bearer ${currentUser.token}` },
              })
          : await api<Car[]>("/cars");
      const loaded = data || [];
      setCars(loaded);
      onCarsLoaded(loaded);
      setCarsLoadStatus("ok");
      onCarsLoadStatus?.("ok");
    } catch (e) {
      setCarsLoadStatus("error");
      onCarsLoadStatus?.("error");
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    void loadCars();
  }, [mode, currentUser?.token]);

  useEffect(() => {
    if (!selectedBrandFromHero?.trim()) return;
    const next = {
      ...BUYER_FILTERS_INITIAL,
      brand: selectedBrandFromHero.trim(),
      brandMode: "include" as const,
    };
    setFilters(next);
    setAppliedFilters(next);
  }, [selectedBrandFromHero]);

  useEffect(() => {
    setSelectedCar(null);
  }, [resetToListSignal]);

  useEffect(() => {
    if (!buyerOpenCarRequest) return;
    if (mode !== "buyer") {
      onBuyerOpenCarRequestHandled();
      return;
    }
    if (carsLoadStatus === "error") {
      onBuyerOpenCarRequestHandled();
      return;
    }
    if (carsLoadStatus !== "ok") return;
    const found = cars.find((c) => c.id === buyerOpenCarRequest.id);
    if (found) {
      setSelectedCar(found);
    }
    onBuyerOpenCarRequestHandled();
  }, [buyerOpenCarRequest, mode, carsLoadStatus, cars, onBuyerOpenCarRequestHandled]);

  useEffect(() => {
    onBuyerCarDetailOpen?.(mode === "buyer" && selectedCar != null);
  }, [mode, selectedCar, onBuyerCarDetailOpen]);

  useEffect(() => {
    if (!heroModelNavigate || mode !== "buyer") return;
    if (carsLoadStatus !== "ok") return;
    const { nonce, brand, model } = heroModelNavigate;
    if (lastHeroModelNavNonceRef.current === nonce) return;

    const b = brand.trim().toLowerCase();
    const m = model.trim().toLowerCase();
    const matching = cars.filter(
      (c) => c.brand.trim().toLowerCase() === b && c.model.trim().toLowerCase() === m
    );

    if (matching.length === 0) {
      lastHeroModelNavNonceRef.current = nonce;
      return;
    }

    lastHeroModelNavNonceRef.current = nonce;

    if (matching.length === 1) {
      setSelectedCar(matching[0]);
      return;
    }

    setSelectedCar(null);
    const next = {
      ...BUYER_FILTERS_INITIAL,
      brand: brand.trim(),
      brandMode: "include" as const,
      model: model.trim(),
    };
    setFilters(next);
    setAppliedFilters({ ...next, selectedFeatures: [...next.selectedFeatures] });
  }, [heroModelNavigate, mode, cars, carsLoadStatus]);

  useEffect(() => {
    const onPointerDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (buyerBrandPickerRef.current && !buyerBrandPickerRef.current.contains(target)) {
        setBrandPickerBuyerOpen(false);
      }
      if (buyerModelPickerRef.current && !buyerModelPickerRef.current.contains(target)) {
        setModelPickerBuyerOpen(false);
      }
    };

    document.addEventListener("mousedown", onPointerDown);
    return () => document.removeEventListener("mousedown", onPointerDown);
  }, []);

  const filteredCars = useMemo(() => {
    const parseNumberFilter = (value: string): number | null => {
      const normalized = value.trim();
      if (!normalized) return null;
      const parsed = Number(normalized);
      return Number.isFinite(parsed) ? parsed : null;
    };

    const yearFrom = parseNumberFilter(appliedFilters.yearFrom);
    const yearTo = parseNumberFilter(appliedFilters.yearTo);
    const priceFrom = parseNumberFilter(appliedFilters.priceFrom);
    const priceTo = parseNumberFilter(appliedFilters.priceTo);
    const hasPriceFilter = priceFrom !== null || priceTo !== null;
    const filterCur = appliedFilters.priceCurrency;

    const pickerMatches = (carVal: string | null | undefined, filterVal: string) => {
      const f = filterVal.trim();
      if (!f) return true;
      const c = carVal?.trim();
      if (!c) return false;
      return c.toLowerCase() === f.toLowerCase();
    };

    return cars.filter((car) => {
      const byBrand = (() => {
        const sel = appliedFilters.brand.trim();
        if (!sel) return true;
        const same =
          car.brand.trim().toLowerCase() === sel.toLowerCase();
        return appliedFilters.brandMode === "exclude" ? !same : same;
      })();
      const byModel = !appliedFilters.model || car.model.toLowerCase().includes(appliedFilters.model.toLowerCase());
      const byYearFrom = yearFrom === null || car.year >= yearFrom;
      const byYearTo = yearTo === null || car.year <= yearTo;
      const carCur = listingCurrencyLabel(car.priceCurrency);
      const byPriceRange =
        !hasPriceFilter ||
        (carCur === filterCur &&
          (priceFrom === null || car.price >= priceFrom) &&
          (priceTo === null || car.price <= priceTo));
      const byColor = pickerMatches(car.color, appliedFilters.bodyColor);
      const byInteriorColor = pickerMatches(car.interiorColor, appliedFilters.interiorColor);
      const byInteriorMaterial = pickerMatches(car.interiorMaterial, appliedFilters.interiorMaterial);
      const byTransmission = pickerMatches(car.transmission, appliedFilters.transmission);
      const byBodyType = pickerMatches(car.bodyType, appliedFilters.bodyType);
      const byEngineType = pickerMatches(car.engineType, appliedFilters.engineType);
      const byDriveType = pickerMatches(car.driveType, appliedFilters.driveType);
      const byFeatures =
        !appliedFilters.selectedFeatures.length ||
        appliedFilters.selectedFeatures.every((feature) =>
          (car.featureNames || []).some((carFeature) => carFeature.toLowerCase() === feature.toLowerCase())
        );
      return (
        byBrand &&
        byModel &&
        byYearFrom &&
        byYearTo &&
        byPriceRange &&
        byColor &&
        byInteriorColor &&
        byInteriorMaterial &&
        byTransmission &&
        byBodyType &&
        byEngineType &&
        byDriveType &&
        byFeatures
      );
    });
  }, [cars, appliedFilters]);

  /** Продавец — порядок как с API; покупатель — фильтр + сортировка */
  const listingsToShow = useMemo(() => {
    if (mode === "seller") {
      return cars;
    }
    const list = [...filteredCars];
    const byn = (c: Car) => carDualAmounts(c.price, c.priceCurrency).byn;
    const mileageVal = (c: Car) =>
      c.mileage != null && c.mileage >= 0 ? c.mileage : Number.MAX_SAFE_INTEGER;

    switch (buyerSort) {
      case "price_asc":
        list.sort((a, b) => byn(a) - byn(b) || a.id - b.id);
        break;
      case "price_desc":
        list.sort((a, b) => byn(b) - byn(a) || a.id - b.id);
        break;
      case "date_desc":
        list.sort((a, b) => {
          const d = carPublishedSortKeyMs(b) - carPublishedSortKeyMs(a);
          return d !== 0 ? d : b.id - a.id;
        });
        break;
      case "date_asc":
        list.sort((a, b) => {
          const d = carPublishedSortKeyMs(a) - carPublishedSortKeyMs(b);
          return d !== 0 ? d : a.id - b.id;
        });
        break;
      case "mileage_asc":
        list.sort((a, b) => mileageVal(a) - mileageVal(b) || a.id - b.id);
        break;
      case "year_desc":
        list.sort((a, b) => b.year - a.year || b.id - a.id);
        break;
      case "year_asc":
        list.sort((a, b) => a.year - b.year || a.id - b.id);
        break;
      default:
        break;
    }
    return list;
  }, [mode, cars, filteredCars, buyerSort]);

  const listingsTotalPages = Math.max(1, Math.ceil(listingsToShow.length / LISTINGS_PER_PAGE));

  useEffect(() => {
    setListingsPage(1);
  }, [mode, buyerSort, appliedFilters]);

  useEffect(() => {
    if (listingsPage > listingsTotalPages) {
      setListingsPage(listingsTotalPages);
    }
  }, [listingsPage, listingsTotalPages]);

  useEffect(() => {
    if (!paginationScrollIntent) return;
    if (paginationScrollIntent === "top") {
      window.scrollTo({ top: 0, behavior: "auto" });
    } else {
      listingsBottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
    }
    setPaginationScrollIntent(null);
  }, [listingsPage, paginationScrollIntent]);

  const listingsPageItems = useMemo(() => {
    const start = (listingsPage - 1) * LISTINGS_PER_PAGE;
    return listingsToShow.slice(start, start + LISTINGS_PER_PAGE);
  }, [listingsToShow, listingsPage]);

  const goToPrevListingsPage = (e?: ReactMouseEvent<HTMLButtonElement>) => {
    if (listingsPage === 1) return;
    e?.currentTarget.blur();
    setPaginationScrollIntent("bottom");
    setListingsPage((p) => Math.max(1, p - 1));
  };

  const goToNextListingsPage = (e?: ReactMouseEvent<HTMLButtonElement>) => {
    if (listingsPage === listingsTotalPages) return;
    e?.currentTarget.blur();
    window.scrollTo({ top: 0, behavior: "auto" });
    setPaginationScrollIntent("top");
    setListingsPage((p) => Math.min(listingsTotalPages, p + 1));
  };

  const carForPhotos = useMemo(
    () =>
      photoCarIdResolved != null ? cars.find((c) => c.id === photoCarIdResolved) : undefined,
    [cars, photoCarIdResolved]
  );

  const uniqueBrands = useMemo(() => {
    const set = new Set<string>();
    cars.forEach((c) => {
      if (c.brand?.trim()) set.add(c.brand.trim());
    });
    return [...set].sort((a, b) => a.localeCompare(b, "ru"));
  }, [cars]);

  const brandsForBuyerPanel = useMemo(() => {
    const q = brandPanelSearch.trim().toLowerCase();
    if (!q) return uniqueBrands;
    return uniqueBrands.filter((b) => b.toLowerCase().includes(q));
  }, [uniqueBrands, brandPanelSearch]);

  const modelsForSelectedBrand = useMemo(() => {
    const selectedBrand = filters.brand.trim().toLowerCase();
    if (!selectedBrand) return [];
    const set = new Set<string>();
    cars.forEach((car) => {
      if (car.brand.trim().toLowerCase() !== selectedBrand) return;
      if (car.model?.trim()) set.add(car.model.trim());
    });
    return [...set].sort((a, b) => a.localeCompare(b, "ru"));
  }, [cars, filters.brand]);

  const modelsForBuyerPanel = useMemo(() => {
    const q = modelPanelSearch.trim().toLowerCase();
    if (!q) return modelsForSelectedBrand;
    return modelsForSelectedBrand.filter((m) => m.toLowerCase().includes(q));
  }, [modelsForSelectedBrand, modelPanelSearch]);

  const bodyColorPickerOptions = useMemo((): PickerOption[] => {
    const set = new Set<string>();
    cars.forEach((c) => {
      if (c.color?.trim()) set.add(c.color.trim());
    });
    const sorted = [...set].sort((a, b) => a.localeCompare(b, "ru"));
    return [{ value: "", label: "Любой" }, ...sorted.map((v) => ({ value: v, label: v }))];
  }, [cars]);

  const interiorColorPickerOptions = useMemo((): PickerOption[] => {
    const set = new Set<string>();
    cars.forEach((c) => {
      if (c.interiorColor?.trim()) set.add(c.interiorColor.trim());
    });
    const sorted = [...set].sort((a, b) => a.localeCompare(b, "ru"));
    return [{ value: "", label: "Любой" }, ...sorted.map((v) => ({ value: v, label: v }))];
  }, [cars]);

  const interiorMaterialPickerOptions = useMemo((): PickerOption[] => {
    const set = new Set<string>();
    DEFAULT_INTERIOR_MATERIALS.forEach((material) => set.add(material));
    cars.forEach((c) => {
      if (c.interiorMaterial?.trim()) set.add(c.interiorMaterial.trim());
    });
    const sorted = [...set].sort((a, b) => a.localeCompare(b, "ru"));
    return [{ value: "", label: "Любой" }, ...sorted.map((v) => ({ value: v, label: v }))];
  }, [cars]);

  const sellerMaterialOptions = useMemo((): PickerOption[] => {
    const base = interiorMaterialPickerOptions.filter((o) => o.value !== "");
    const v = form.interiorMaterial.trim();
    if (v && !base.some((o) => o.value === v)) {
      return [{ value: v, label: v }, ...base];
    }
    return base;
  }, [interiorMaterialPickerOptions, form.interiorMaterial]);

  const sellerBodyColorOptions = useMemo((): PickerOption[] => {
    const v = form.color.trim();
    if (v && !SELLER_BODY_COLOR_OPTIONS.some((o) => o.value === v)) {
      return [{ value: v, label: v }, ...SELLER_BODY_COLOR_OPTIONS];
    }
    return SELLER_BODY_COLOR_OPTIONS;
  }, [form.color]);

  const sellerInteriorColorOptions = useMemo((): PickerOption[] => {
    const v = form.interiorColor.trim();
    if (v && !SELLER_INTERIOR_COLOR_OPTIONS.some((o) => o.value === v)) {
      return [{ value: v, label: v }, ...SELLER_INTERIOR_COLOR_OPTIONS];
    }
    return SELLER_INTERIOR_COLOR_OPTIONS;
  }, [form.interiorColor]);

  useEffect(() => {
    if (!filters.brand.trim()) {
      if (filters.model) {
        setFilters((prev) => ({ ...prev, model: "" }));
      }
      setModelPickerBuyerOpen(false);
      setModelPanelSearch("");
      return;
    }

    const modelExists = modelsForSelectedBrand.some(
      (model) => model.toLowerCase() === filters.model.trim().toLowerCase()
    );
    if (filters.model && !modelExists) {
      setFilters((prev) => ({ ...prev, model: "" }));
    }
  }, [filters.brand, filters.model, modelsForSelectedBrand]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (mode === "seller" && !currentUser) {
      setError("Войдите в аккаунт, чтобы публиковать объявления.");
      return;
    }
    setError("");
    setMessage("");
    const rowFields: DealershipBulkRowFields = {
      brand: form.brand,
      model: form.model,
      year: form.year,
      city: form.city,
      color: form.color,
      interiorColor: form.interiorColor,
      interiorMaterial: form.interiorMaterial,
      engineVolume: form.engineVolume,
      mileage: form.mileage,
      powerHp: form.powerHp,
      fuelConsumptionCity: form.fuelConsumptionCity,
      fuelConsumptionHighway: form.fuelConsumptionHighway,
      fuelConsumptionMixed: form.fuelConsumptionMixed,
      seats: form.seats,
      price: form.price,
      transmission: sellerExtras.transmission,
      bodyType: sellerExtras.bodyType,
      engineType: sellerExtras.engineType,
      driveType: sellerExtras.driveType,
      ownerUserId: form.ownerUserId,
    };
    const nameToId = new Map(featureCatalog.map((f) => [f.name.trim().toLowerCase(), f.id]));
    const featureIds = sellerExtras.selectedFeatures
      .map((n) => nameToId.get(n.trim().toLowerCase()))
      .filter((x): x is number => x != null);
    const built = buildCarPayloadFromSellerFields(rowFields, {
      priceCurrency: form.priceCurrency,
      featureIds,
    });
    if (!built.ok) {
      setError(built.message);
      return;
    }
    const payload = built.payload;
    try {
      if (editingId) {
        const savedListingId = editingId;
        await api(`/cars/${savedListingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
          headers: { Authorization: `Bearer ${currentUser?.token || ""}` },
        });
        setMessage("Объявление обновлено. При необходимости снова нажмите «Изменить» в списке — форма откроется в окне.");
        setPhotoUploadCarId(null);
        setEditingId(null);
        setForm(emptyForm);
        setSellerExtras({ ...BUYER_FILTERS_INITIAL });
        await loadCars();
      } else {
        const created = await api<Car>("/cars", {
          method: "POST",
          body: JSON.stringify(payload),
          headers: { Authorization: `Bearer ${currentUser?.token || ""}` },
        });
        const photoFocusId = created.id;
        let createMsg = "Объявление добавлено.";
        if (createPhotoFiles.length > 0 && currentUser?.token) {
          const uploaded = await uploadPhotosForCar(photoFocusId, createPhotoFiles, currentUser.token);
          createMsg =
            uploaded > 0
              ? `Объявление добавлено. Загружено фотографий: ${uploaded}.`
              : "Объявление добавлено. Фото не загружены: сервер вернул пустой список.";
        }
        if (createPhotoInputRef.current) createPhotoInputRef.current.value = "";
        setCreatePhotoFiles([]);
        setMessage(createMsg);
        setPhotoBanner({ kind: "ok", text: createMsg });
        setPhotoUploadCarId(photoFocusId);
        setEditingId(null);
        setForm(emptyForm);
        setSellerExtras({ ...BUYER_FILTERS_INITIAL });
        await loadCars();
        setPhotoUploadCarId(photoFocusId);
      }
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const startEdit = (car: Car) => {
    setEditingId(car.id);
    setPhotoUploadCarId(null);
    setForm({
      brand: car.brand,
      model: car.model,
      year: String(car.year),
      city: car.city ?? "",
      color: car.color,
      interiorColor: car.interiorColor ?? "",
      interiorMaterial: car.interiorMaterial ?? "",
      engineVolume: car.engineVolume != null ? String(car.engineVolume) : "",
      mileage: car.mileage != null ? String(car.mileage) : "",
      powerHp: car.powerHp != null ? String(car.powerHp) : "",
      fuelConsumptionCity: car.fuelConsumptionCity != null ? String(car.fuelConsumptionCity) : "",
      fuelConsumptionHighway: car.fuelConsumptionHighway != null ? String(car.fuelConsumptionHighway) : "",
      fuelConsumptionMixed: car.fuelConsumptionMixed != null ? String(car.fuelConsumptionMixed) : "",
      seats: car.seatCount != null ? String(car.seatCount) : "",
      price: String(car.price),
      priceCurrency: listingCurrencyLabel(car.priceCurrency),
      ownerUserId: "",
    });
    setSellerExtras({
      ...BUYER_FILTERS_INITIAL,
      transmission: car.transmission ?? "",
      bodyType: car.bodyType ?? "",
      engineType: car.engineType ?? "",
      driveType: car.driveType ?? "",
      selectedFeatures: [...(car.featureNames ?? [])],
    });
  };

  const cancelSellerEdit = useCallback(() => {
    setEditingId(null);
    setPhotoUploadCarId(null);
    setCreatePhotoFiles([]);
    if (createPhotoInputRef.current) createPhotoInputRef.current.value = "";
    setForm(emptyForm);
    setSellerExtras({ ...BUYER_FILTERS_INITIAL });
    setOpenedSellerSection(null);
  }, []);

  useEffect(() => {
    if (editingId === null) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") cancelSellerEdit();
    };
    document.addEventListener("keydown", onKeyDown);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = prevOverflow;
    };
  }, [editingId, cancelSellerEdit]);

  const deleteCar = async (id: number) => {
    if (!window.confirm("Удалить объявление?")) return;
    try {
      await api(`/cars/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${currentUser?.token || ""}` },
      });
      setMessage("Объявление удалено");
      setError("");
      if (photoUploadCarId === id) setPhotoUploadCarId(null);
      if (editingId === id) cancelSellerEdit();
      await loadCars();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const uploadPhotosForCar = async (id: number, fileList: File[], token: string): Promise<number> => {
    const fd = new FormData();
    fileList.forEach((f) => fd.append("files", f));
    const base = API_BASE.replace(/\/$/, "");
    const res = await fetch(`${base}/cars/${Number(id)}/photos`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: fd,
    });
    const raw = await res.text();
    let parsed: unknown = null;
    try {
      parsed = raw ? JSON.parse(raw) : null;
    } catch {
      parsed = null;
    }
    if (!res.ok) {
      const errBody =
        parsed && typeof parsed === "object" && parsed !== null
          ? (parsed as { message?: string; error?: string })
          : {};
      throw new Error(
        errBody.message ||
          errBody.error ||
          raw?.slice(0, 200) ||
          `Ошибка ${res.status}: загрузка фото`
      );
    }
    return Array.isArray(parsed) ? parsed.length : 0;
  };

  const handleSellerPhotoInput = async (e: ChangeEvent<HTMLInputElement>) => {
    if (photoUploadLockRef.current || photoBusy) {
      e.target.value = "";
      return;
    }
    const id = photoCarIdResolved;
    const input = e.target;
    /** Снимок файлов ДО сброса value: иначе FileList в ряде браузеров опустошается и multipart уходит пустым. */
    const fileList = input.files ? Array.from(input.files) : [];
    input.value = "";
    if (fileList.length === 0) return;
    const token = currentUser?.token?.trim();
    if (!token) {
      setError("Войдите в аккаунт, чтобы загружать фото.");
      setPhotoBanner({ kind: "err", text: "Войдите в аккаунт, чтобы загружать фото." });
      return;
    }
    if (!id) {
      const hint =
        "Сначала сохраните объявление или откройте его редактирование в таблице — фото привязаны к выбранной карточке.";
      setError(hint);
      setPhotoBanner({ kind: "err", text: hint });
      return;
    }
    photoUploadLockRef.current = true;
    setPhotoBusy(true);
    setError("");
    setPhotoBanner(null);
    try {
      const n = await uploadPhotosForCar(id, fileList, token);
      const okText =
        n > 0
          ? `Готово: загружено фотографий — ${n}. Они появятся в таблице и в превью ниже.`
          : "Сервер принял запрос, но список файлов пуст — проверьте формат (JPG, PNG, WebP).";
      setMessage(okText);
      setPhotoBanner({ kind: "ok", text: okText });
      await loadCars();
    } catch (err) {
      const msg = (err as Error).message;
      setError(msg);
      setPhotoBanner({ kind: "err", text: msg });
    } finally {
      photoUploadLockRef.current = false;
      setPhotoBusy(false);
    }
  };

  const handleCreatePhotoSelection = (e: ChangeEvent<HTMLInputElement>) => {
    const fileList = e.target.files ? Array.from(e.target.files) : [];
    if (fileList.length === 0) {
      setCreatePhotoFiles([]);
      return;
    }
    setCreatePhotoFiles(fileList.slice(0, 10));
  };

  const deleteSellerPhoto = async (carId: number, imageId: number) => {
    if (!currentUser?.token) return;
    if (!window.confirm("Удалить это фото?")) return;
    try {
      const res = await fetch(`${API_BASE}/cars/${carId}/photos/${imageId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${currentUser.token}` },
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data?.message || data?.error || "Не удалось удалить фото");
      }
      setMessage("Фото удалено.");
      await loadCars();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const submitDealershipBulk = async () => {
    if (
      !currentUser?.token ||
      (currentUser.accountType !== "dealership" && currentUser.accountType !== "admin")
    )
      return;
    setBulkMessage("");
    setBulkError("");
    const payloads: CarCreateApiPayload[] = [];
    for (let i = 0; i < bulkRows.length; i++) {
      const row = bulkRows[i];
      const fields: DealershipBulkRowFields = {
        brand: row.brand,
        model: row.model,
        year: row.year,
        city: row.city,
        color: row.color,
        interiorColor: row.interiorColor,
        interiorMaterial: row.interiorMaterial,
        engineVolume: row.engineVolume,
        mileage: row.mileage,
        powerHp: row.powerHp,
        fuelConsumptionCity: row.fuelConsumptionCity,
        fuelConsumptionHighway: row.fuelConsumptionHighway,
        fuelConsumptionMixed: row.fuelConsumptionMixed,
        seats: row.seats,
        price: row.price,
        transmission: row.transmission,
        bodyType: row.bodyType,
        engineType: row.engineType,
        driveType: row.driveType,
        ownerUserId: row.ownerUserId,
      };
      const built = buildCarPayloadFromSellerFields(fields, { priceCurrency: bulkPriceCurrency, featureIds: [] });
      if (!built.ok) {
        setBulkError(`Позиция ${i + 1}: ${built.message}`);
        return;
      }
      payloads.push(built.payload);
    }
    setBulkBusy(true);
    try {
      const created = await api<Car[]>("/cars/bulk/dealership", {
        method: "POST",
        headers: { Authorization: `Bearer ${currentUser.token}` },
        body: JSON.stringify({ cars: payloads }),
      });
      setBulkMessage(`Добавлено объявлений: ${created?.length ?? 0}.`);
      let bulkPhotoFocusId: number | null = null;
      if (created && created.length > 0) {
        const last = created[created.length - 1];
        bulkPhotoFocusId = last.id;
        setPhotoUploadCarId(last.id);
      }
      bulkRowIdRef.current = 0;
      setBulkRows([{ id: ++bulkRowIdRef.current, ...emptyDealershipBulkRowFields() }]);
      await loadCars();
      if (bulkPhotoFocusId != null) {
        setPhotoUploadCarId(bulkPhotoFocusId);
      }
    } catch (err) {
      setBulkError((err as Error).message);
    } finally {
      setBulkBusy(false);
    }
  };

  const updateBulkRow = (id: number, patch: Partial<DealershipBulkRowFields>) => {
    setBulkRows((rows) => rows.map((r) => (r.id === id ? { ...r, ...patch } : r)));
  };

  const addBulkRow = () => {
    setBulkRows((rows) => {
      if (rows.length >= MAX_DEALERSHIP_BULK_ROWS) return rows;
      return [...rows, { id: ++bulkRowIdRef.current, ...emptyDealershipBulkRowFields() }];
    });
    setBulkError("");
    setBulkMessage("");
  };

  const removeBulkRow = (id: number) => {
    setBulkRows((rows) => {
      if (rows.length <= 1) return rows;
      return rows.filter((r) => r.id !== id);
    });
  };

  const appendBulkRowFromMainForm = () => {
    setBulkRows((rows) => {
      if (rows.length >= MAX_DEALERSHIP_BULK_ROWS) return rows;
      return [
        ...rows,
        {
          id: ++bulkRowIdRef.current,
          brand: form.brand,
          model: form.model,
          year: form.year,
          city: form.city,
          color: form.color,
          interiorColor: form.interiorColor,
          interiorMaterial: form.interiorMaterial,
          engineVolume: form.engineVolume,
          mileage: form.mileage,
          powerHp: form.powerHp,
          fuelConsumptionCity: form.fuelConsumptionCity,
          fuelConsumptionHighway: form.fuelConsumptionHighway,
          fuelConsumptionMixed: form.fuelConsumptionMixed,
          seats: form.seats,
          price: form.price,
          transmission: sellerExtras.transmission,
          bodyType: sellerExtras.bodyType,
          engineType: sellerExtras.engineType,
          driveType: sellerExtras.driveType,
          ownerUserId: form.ownerUserId,
        },
      ];
    });
    setBulkError("");
    setBulkMessage("");
  };

  const bulkRowMaterialOptions = (row: DealershipBulkRow): PickerOption[] => {
    const base = interiorMaterialPickerOptions.filter((o) => o.value !== "");
    const v = row.interiorMaterial.trim();
    if (v && !base.some((o) => o.value === v)) {
      return [{ value: v, label: v }, ...base];
    }
    return base;
  };

  const bulkRowBodyColorOptions = (row: DealershipBulkRow): PickerOption[] => {
    const v = row.color.trim();
    if (v && !SELLER_BODY_COLOR_OPTIONS.some((o) => o.value === v)) {
      return [{ value: v, label: v }, ...SELLER_BODY_COLOR_OPTIONS];
    }
    return SELLER_BODY_COLOR_OPTIONS;
  };

  const bulkRowInteriorColorOptions = (row: DealershipBulkRow): PickerOption[] => {
    const v = row.interiorColor.trim();
    if (v && !SELLER_INTERIOR_COLOR_OPTIONS.some((o) => o.value === v)) {
      return [{ value: v, label: v }, ...SELLER_INTERIOR_COLOR_OPTIONS];
    }
    return SELLER_INTERIOR_COLOR_OPTIONS;
  };

  return (
    <>
      {selectedCar && mode === "buyer" ? (
        <CarDetailsPage
          car={selectedCar}
          onBack={() => setSelectedCar(null)}
          isFavorite={favoriteCarIds.has(selectedCar.id)}
          onFavoriteToggle={() => onFavoriteToggle(selectedCar.id)}
          viewerPhone={currentUser?.username ?? null}
        />
      ) : (
        <>
      {mode === "buyer" && (
        <section className="search-panel">
          <div className="search-panel-head">
            <h2>Поиск по параметрам</h2>
          </div>
          <div className="search-table">
            <div className="search-grid">
              <div className="search-cell">
                <span className="search-cell-label">Марка</span>
                <div
                  ref={buyerBrandPickerRef}
                  className={`brand-picker ${brandPickerBuyerOpen ? "is-open" : ""}`}
                  onMouseEnter={() => setBrandPickerBuyerOpen(true)}
                  onMouseLeave={() => {
                    setBrandPickerBuyerOpen(false);
                    setBrandPanelSearch("");
                  }}
                >
                  <button
                    type="button"
                    className="brand-picker-trigger"
                    onClick={() => setBrandPickerBuyerOpen((prev) => !prev)}
                    onFocus={() => setBrandPickerBuyerOpen(true)}
                  >
                    <span className="brand-picker-trigger-label">
                      {filters.brand.trim() || "Марка"}
                    </span>
                    <span className="brand-picker-chevron" aria-hidden>
                      ▾
                    </span>
                  </button>
                  {brandPickerBuyerOpen && (
                    <div className="brand-picker-panel">
                      <div className="brand-mode-row">
                        <label className="brand-mode-option">
                          <input
                            type="radio"
                            name="brandMode"
                            checked={filters.brandMode === "include"}
                            onChange={() =>
                              setFilters({ ...filters, brandMode: "include" })
                            }
                          />
                          <span>Выбрать</span>
                        </label>
                        <label className="brand-mode-option">
                          <input
                            type="radio"
                            name="brandMode"
                            checked={filters.brandMode === "exclude"}
                            onChange={() =>
                              setFilters({ ...filters, brandMode: "exclude" })
                            }
                          />
                          <span>Исключить</span>
                        </label>
                      </div>
                      <input
                        className="brand-panel-search"
                        placeholder="Поиск"
                        value={brandPanelSearch}
                        onChange={(e) => setBrandPanelSearch(e.target.value)}
                        autoComplete="off"
                      />
                      <ul className="brand-dropdown" role="listbox">
                        <li>
                          <button
                            type="button"
                            className="brand-dropdown-item"
                            onClick={() => {
                              setFilters({ ...filters, brand: "", brandMode: "include" });
                              setBrandPickerBuyerOpen(false);
                              setBrandPanelSearch("");
                            }}
                          >
                            Любой
                          </button>
                        </li>
                        {carsLoadStatus === "loading" && (
                          <li className="brand-dropdown-empty">Загрузка марок…</li>
                        )}
                        {carsLoadStatus === "error" && (
                          <li className="brand-dropdown-empty">
                            Не удалось загрузить {API_BASE}/cars. Запустите backend и проверьте
                            CORS для фронта (например http://localhost:5173).
                          </li>
                        )}
                        {carsLoadStatus === "ok" &&
                          uniqueBrands.length === 0 &&
                          brandsForBuyerPanel.length === 0 && (
                            <li className="brand-dropdown-empty">
                              API вернул 0 машин — в таблице cars пусто или DataInitializer
                              пропустил createCars (уже есть записи в carRepository).
                            </li>
                          )}
                        {carsLoadStatus === "ok" &&
                          uniqueBrands.length > 0 &&
                          brandsForBuyerPanel.length === 0 && (
                            <li className="brand-dropdown-empty">Ничего не найдено по поиску</li>
                          )}
                        {brandsForBuyerPanel.map((b) => (
                          <li key={b}>
                            <button
                              type="button"
                              className="brand-dropdown-item"
                              onClick={() => {
                                setFilters({ ...filters, brand: b });
                                setBrandPickerBuyerOpen(false);
                                setBrandPanelSearch("");
                              }}
                            >
                              {b}
                            </button>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
              <div className="search-cell">
                <span className="search-cell-label">Модель</span>
                <div
                  ref={buyerModelPickerRef}
                  className={`brand-picker ${modelPickerBuyerOpen ? "is-open" : ""}`}
                  onMouseEnter={() => {
                    if (filters.brand.trim()) setModelPickerBuyerOpen(true);
                  }}
                  onMouseLeave={() => {
                    setModelPickerBuyerOpen(false);
                    setModelPanelSearch("");
                  }}
                >
                  <button
                    type="button"
                    className="brand-picker-trigger"
                    disabled={!filters.brand.trim()}
                    onClick={() => {
                      if (!filters.brand.trim()) return;
                      setModelPickerBuyerOpen((prev) => !prev);
                    }}
                    onFocus={() => {
                      if (filters.brand.trim()) setModelPickerBuyerOpen(true);
                    }}
                  >
                    <span className="brand-picker-trigger-label">
                      {filters.model.trim() || "Любая"}
                    </span>
                    <span className="brand-picker-chevron" aria-hidden>
                      ▾
                    </span>
                  </button>
                  {modelPickerBuyerOpen && (
                    <div className="brand-picker-panel">
                      <input
                        className="brand-panel-search"
                        placeholder="Поиск"
                        value={modelPanelSearch}
                        onChange={(e) => setModelPanelSearch(e.target.value)}
                        autoComplete="off"
                      />
                      <ul className="brand-dropdown" role="listbox">
                        <li>
                          <button
                            type="button"
                            className="brand-dropdown-item"
                            onClick={() => {
                              setFilters({ ...filters, model: "" });
                              setModelPickerBuyerOpen(false);
                              setModelPanelSearch("");
                            }}
                          >
                            Любая
                          </button>
                        </li>
                        {modelsForSelectedBrand.length === 0 && (
                          <li className="brand-dropdown-empty">Нет моделей для выбранной марки</li>
                        )}
                        {modelsForSelectedBrand.length > 0 && modelsForBuyerPanel.length === 0 && (
                          <li className="brand-dropdown-empty">Ничего не найдено по поиску</li>
                        )}
                        {modelsForBuyerPanel.map((model) => (
                          <li key={model}>
                            <button
                              type="button"
                              className="brand-dropdown-item"
                              onClick={() => {
                                setFilters({ ...filters, model });
                                setModelPickerBuyerOpen(false);
                                setModelPanelSearch("");
                              }}
                            >
                              {model}
                            </button>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
              <div className="search-cell">
                <span className="search-cell-label">Год</span>
                <div className="double-input">
                  <input type="number" min={MIN_CAR_MODEL_YEAR} max={MAX_CAR_MODEL_YEAR} placeholder="от" value={filters.yearFrom} onChange={(e) => setFilters({ ...filters, yearFrom: normalizeYearInput(e.target.value) })} />
                  <input type="number" min={MIN_CAR_MODEL_YEAR} max={MAX_CAR_MODEL_YEAR} placeholder="до" value={filters.yearTo} onChange={(e) => setFilters({ ...filters, yearTo: normalizeYearInput(e.target.value) })} />
                </div>
              </div>
              <div className="search-cell">
                <span className="search-cell-label">Цена</span>
                <div className="price-filter-row">
                  <div className="double-input">
                    <input type="number" min={1} max={MAX_CAR_PRICE} step="any" placeholder="от" value={filters.priceFrom} onChange={(e) => setFilters({ ...filters, priceFrom: e.target.value })} />
                    <input type="number" min={1} max={MAX_CAR_PRICE} step="any" placeholder="до" value={filters.priceTo} onChange={(e) => setFilters({ ...filters, priceTo: e.target.value })} />
                  </div>
                  <SimpleFilterPicker
                    compact
                    value={filters.priceCurrency}
                    placeholder="USD"
                    options={LISTING_CURRENCY_OPTIONS}
                    onChange={(value) =>
                      setFilters({ ...filters, priceCurrency: value as ListingCurrency })
                    }
                  />
                </div>
              </div>
            </div>
            <div className="quick-filters-grid">
              <div className="search-cell">
                <span className="search-cell-label">Коробка передач</span>
                <SimpleFilterPicker
                  value={filters.transmission}
                  placeholder="Любая"
                  onChange={(value) => setFilters({ ...filters, transmission: value })}
                  options={[
                    { value: "", label: "Любая" },
                    { value: "auto", label: "Автомат" },
                    { value: "manual", label: "Механика" },
                    { value: "robot", label: "Робот" },
                  ]}
                />
              </div>
              <div className="search-cell">
                <span className="search-cell-label">Кузов</span>
                <SimpleFilterPicker
                  value={filters.bodyType}
                  placeholder="Любой"
                  onChange={(value) => setFilters({ ...filters, bodyType: value })}
                  options={[
                    { value: "", label: "Любой" },
                    { value: "sedan", label: "Седан" },
                    { value: "suv", label: "SUV" },
                    { value: "hatchback", label: "Хэтчбек" },
                    { value: "wagon", label: "Универсал" },
                    { value: "coupe", label: "Купе" },
                    { value: "cabriolet", label: "Кабриолет" },
                  ]}
                />
              </div>
              <div className="search-cell">
                <span className="search-cell-label">Тип двигателя</span>
                <SimpleFilterPicker
                  value={filters.engineType}
                  placeholder="Любой"
                  onChange={(value) => setFilters({ ...filters, engineType: value })}
                  options={[
                    { value: "", label: "Любой" },
                    { value: "petrol", label: "Бензин" },
                    { value: "diesel", label: "Дизель" },
                    { value: "electric", label: "Электро" },
                  ]}
                />
              </div>
              <div className="search-cell">
                <span className="search-cell-label">Привод</span>
                <SimpleFilterPicker
                  value={filters.driveType}
                  placeholder="Любой"
                  onChange={(value) => setFilters({ ...filters, driveType: value })}
                  options={[
                    { value: "", label: "Любой" },
                    { value: "fwd", label: "Передний" },
                    { value: "rwd", label: "Задний" },
                    { value: "awd", label: "Полный" },
                  ]}
                />
              </div>
            </div>
          </div>
          {showAdvancedFilters && (
            <div className="advanced-filters">
              <div className="advanced-grid numbers">
                <div>
                  <label>Мощность, л.с.</label>
                  <div className="double-input">
                    <input type="number" min={1} max={1500} step={1} placeholder="от" value={filters.powerFrom} onChange={(e) => setFilters({ ...filters, powerFrom: normalizeCappedNumberInput(e.target.value, 1500) })} />
                    <input type="number" min={1} max={1500} step={1} placeholder="до" value={filters.powerTo} onChange={(e) => setFilters({ ...filters, powerTo: normalizeCappedNumberInput(e.target.value, 1500) })} />
                  </div>
                </div>
                <div>
                  <label>Расход, л/100км</label>
                  <div className="double-input">
                    <input type="number" min={0.1} max={100} step={0.1} placeholder="от" value={filters.consumptionFrom} onChange={(e) => setFilters({ ...filters, consumptionFrom: normalizeCappedNumberInput(e.target.value, 100, { allowDecimal: true }) })} />
                    <input type="number" min={0.1} max={100} step={0.1} placeholder="до" value={filters.consumptionTo} onChange={(e) => setFilters({ ...filters, consumptionTo: normalizeCappedNumberInput(e.target.value, 100, { allowDecimal: true }) })} />
                  </div>
                </div>
                <div>
                  <label>Запас хода, км</label>
                  <div className="double-input">
                    <input type="number" min={1} step={1} placeholder="от" value={filters.rangeFrom} onChange={(e) => setFilters({ ...filters, rangeFrom: e.target.value })} />
                    <input type="number" min={1} step={1} placeholder="до" value={filters.rangeTo} onChange={(e) => setFilters({ ...filters, rangeTo: e.target.value })} />
                  </div>
                </div>
              </div>

              <div className="advanced-grid">
                <div>
                  <label>Цвет кузова</label>
                  <SimpleFilterPicker
                    value={filters.bodyColor}
                    placeholder="Любой"
                    options={bodyColorPickerOptions}
                    onChange={(value) => setFilters({ ...filters, bodyColor: value })}
                  />
                </div>
                <div>
                  <label>Цвет салона</label>
                  <SimpleFilterPicker
                    value={filters.interiorColor}
                    placeholder="Любой"
                    options={interiorColorPickerOptions}
                    onChange={(value) => setFilters({ ...filters, interiorColor: value })}
                  />
                </div>
                <div>
                  <label>Материал салона</label>
                  <SimpleFilterPicker
                    value={filters.interiorMaterial}
                    placeholder="Любой"
                    options={interiorMaterialPickerOptions}
                    onChange={(value) => setFilters({ ...filters, interiorMaterial: value })}
                  />
                </div>
                <div>
                  <label>К-во мест</label>
                  <input type="number" min={1} max={9} step={1} placeholder="Любое" value={filters.seats} onChange={(e) => setFilters({ ...filters, seats: e.target.value })} />
                </div>
              </div>

              {featureSections.length === 0 && (
                <p className="search-panel-hint">Список опций не загружен — проверьте, что backend доступен.</p>
              )}
              <div className="feature-accordion">
                {featureSections.map((section) => (
                  <div key={section.title} className="feature-row">
                    <button
                      type="button"
                      className="feature-row-btn"
                      onClick={() =>
                        setOpenedSection((prev) => (prev === section.title ? null : section.title))
                      }
                    >
                      <span>{section.title}</span>
                      <span className={`feature-chevron ${openedSection === section.title ? "open" : ""}`}>
                        ▾
                      </span>
                    </button>
                    {openedSection === section.title && (
                      <div className="feature-row-content">
                        {section.items.map((item) => (
                          <button
                            key={item}
                            type="button"
                            className={`feature-option-chip ${filters.selectedFeatures.includes(item) ? "active" : ""}`}
                            onClick={() =>
                              setFilters((prev) => ({
                                ...prev,
                                selectedFeatures: prev.selectedFeatures.includes(item)
                                  ? prev.selectedFeatures.filter((feature) => feature !== item)
                                  : [...prev.selectedFeatures, item],
                              }))
                            }
                          >
                            {item}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
          <div className="search-actions">
            <button className="secondary" onClick={() => setShowAdvancedFilters((prev) => !prev)}>
              {showAdvancedFilters ? "Скрыть параметры" : "Все параметры"}
            </button>
            <button className="secondary" onClick={resetBuyerFilters}>
              Сбросить
            </button>
            <button
              className="show-results-btn"
              type="button"
              onClick={() =>
                setAppliedFilters({
                  ...filters,
                  selectedFeatures: [...filters.selectedFeatures],
                })
              }
            >
              Показать {filteredCars.length} объявлений
            </button>
          </div>
        </section>
      )}

      {mode === "seller" && (
        <section className={`search-panel${editingId != null ? " search-panel--listing-edit-modal" : ""}`}>
          {editingId != null && (
            <div
              className="listing-edit-backdrop"
              role="presentation"
              aria-hidden
              onClick={() => cancelSellerEdit()}
            />
          )}
          <div
            className={editingId != null ? "listing-edit-modal" : undefined}
            role={editingId != null ? "dialog" : undefined}
            aria-modal={editingId != null ? true : undefined}
            aria-labelledby={editingId != null ? "listing-edit-dialog-title" : undefined}
            onClick={editingId != null ? (e) => e.stopPropagation() : undefined}
          >
            {editingId === null ? (
              <div className="search-panel-head">
                <h2>Добавление авто по параметрам</h2>
                {currentUser?.accountType === "dealership" && (
                  <p className="search-panel-hint">
                    Аккаунт автосалона: одно объявление — форма выше; несколько позиций сразу — таблица «Несколько
                    автомобилей» ниже (до {MAX_DEALERSHIP_BULK_ROWS} шт. за раз).
                  </p>
                )}
              </div>
            ) : (
              <div className="listing-edit-modal-head">
                <h2 className="listing-edit-modal-title" id="listing-edit-dialog-title">
                  Редактирование объявления
                </h2>
                <div className="listing-edit-modal-actions">
                  <button
                    type="button"
                    className="listing-edit-modal-close"
                    aria-label="Закрыть"
                    onClick={() => cancelSellerEdit()}
                  >
                    ×
                  </button>
                </div>
              </div>
            )}
            {editingId != null && message && <p className="message success listing-edit-modal-banner">{message}</p>}
            {editingId != null && error && <p className="message error listing-edit-modal-banner">{error}</p>}
          <form onSubmit={submit}>
            <div className="search-table seller-create-table">
              <div className="search-grid">
                <div className="search-cell">
                  <span className="search-cell-label">Марка</span>
                  <input
                    required
                    placeholder="Марка"
                    value={form.brand}
                    onChange={(e) => setForm({ ...form, brand: e.target.value })}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Модель</span>
                  <input
                    required
                    placeholder="Например, Camry"
                    value={form.model}
                    onChange={(e) => setForm({ ...form, model: e.target.value })}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Год</span>
                  <input
                    required
                    type="number"
                    min={MIN_CAR_MODEL_YEAR}
                    max={MAX_CAR_MODEL_YEAR}
                    placeholder="Год"
                    value={form.year}
                    onChange={(e) => setForm({ ...form, year: normalizeYearInput(e.target.value) })}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Цена</span>
                  <div className="price-filter-row">
                    <input
                      required
                      className="price-amount-input"
                      type="number"
                      min={1}
                      max={MAX_CAR_PRICE}
                      step="any"
                      placeholder="Цена"
                      value={form.price}
                      onChange={(e) => setForm({ ...form, price: e.target.value })}
                    />
                    <SimpleFilterPicker
                      compact
                      value={form.priceCurrency}
                      placeholder="USD"
                      options={LISTING_CURRENCY_OPTIONS}
                      onChange={(value) =>
                        setForm({ ...form, priceCurrency: value as ListingCurrency })
                      }
                    />
                  </div>
                </div>
              </div>
              <div className="search-grid">
                <div className="search-cell">
                  <span className="search-cell-label">Город</span>
                  <input
                    placeholder="Например, Гродно"
                    value={form.city}
                    onChange={(e) => setForm({ ...form, city: e.target.value })}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Объём двигателя, л</span>
                  <input
                    type="number"
                    min={0.1}
                    max={20}
                    step={0.1}
                    placeholder="Например 2.0"
                    value={form.engineVolume}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        engineVolume: normalizeCappedNumberInput(e.target.value, 20, { allowDecimal: true }),
                      })
                    }
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Пробег, км</span>
                  <input
                    type="number"
                    min={0}
                    max={2000000}
                    step={1}
                    placeholder="Например 37000"
                    value={form.mileage}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        mileage: normalizeCappedNumberInput(e.target.value, 2000000),
                      })
                    }
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Мощность, л.с.</span>
                  <input
                    type="number"
                    min={1}
                    max={1500}
                    step={1}
                    placeholder="Мощность"
                    value={form.powerHp}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        powerHp: normalizeCappedNumberInput(e.target.value, 1500),
                      })
                    }
                  />
                </div>
              </div>
              <div className="search-grid">
                <div className="search-cell">
                  <span className="search-cell-label">Расход, л/100км (по городу)</span>
                  <input
                    type="number"
                    min={0.1}
                    max={100}
                    step={0.1}
                    placeholder="По городу"
                    value={form.fuelConsumptionCity}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        fuelConsumptionCity: normalizeCappedNumberInput(e.target.value, 100, {
                          allowDecimal: true,
                        }),
                      })
                    }
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Расход, л/100км (по трассе)</span>
                  <input
                    type="number"
                    min={0.1}
                    max={100}
                    step={0.1}
                    placeholder="По трассе"
                    value={form.fuelConsumptionHighway}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        fuelConsumptionHighway: normalizeCappedNumberInput(e.target.value, 100, {
                          allowDecimal: true,
                        }),
                      })
                    }
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Расход, л/100км (смешанный)</span>
                  <input
                    type="number"
                    min={0.1}
                    max={100}
                    step={0.1}
                    placeholder="Смешанный"
                    value={form.fuelConsumptionMixed}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        fuelConsumptionMixed: normalizeCappedNumberInput(e.target.value, 100, {
                          allowDecimal: true,
                        }),
                      })
                    }
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">К-во мест</span>
                  <input
                    type="number"
                    min={1}
                    max={9}
                    step={1}
                    placeholder="Например 5"
                    value={form.seats}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        seats: normalizeCappedNumberInput(e.target.value, 9),
                      })
                    }
                  />
                </div>
              </div>
              <div className="search-grid search-grid--3">
                <div className="search-cell">
                  <span className="search-cell-label">Цвет кузова</span>
                  <SimpleFilterPicker
                    value={form.color}
                    placeholder="Выберите цвет кузова"
                    options={sellerBodyColorOptions}
                    onChange={(value) => setForm({ ...form, color: value })}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Цвет салона</span>
                  <SimpleFilterPicker
                    value={form.interiorColor}
                    placeholder="Выберите цвет салона"
                    options={sellerInteriorColorOptions}
                    onChange={(value) => setForm({ ...form, interiorColor: value })}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Материал салона</span>
                  <SimpleFilterPicker
                    value={form.interiorMaterial}
                    placeholder="Выберите материал"
                    options={sellerMaterialOptions}
                    onChange={(value) => setForm({ ...form, interiorMaterial: value })}
                  />
                </div>
              </div>
              <div className="quick-filters-grid">
                <div className="search-cell">
                  <span className="search-cell-label">Коробка передач</span>
                  <SimpleFilterPicker
                    value={sellerExtras.transmission}
                    placeholder="Выберите коробку"
                    onChange={(value) =>
                      setSellerExtras((prev) => ({ ...prev, transmission: value }))
                    }
                    options={[
                      { value: "auto", label: "Автомат" },
                      { value: "manual", label: "Механика" },
                      { value: "robot", label: "Робот" },
                    ]}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Тип кузова</span>
                  <SimpleFilterPicker
                    value={sellerExtras.bodyType}
                    placeholder="Выберите тип кузова"
                    onChange={(value) => setSellerExtras((prev) => ({ ...prev, bodyType: value }))}
                    options={[
                      { value: "sedan", label: "Седан" },
                      { value: "suv", label: "SUV" },
                      { value: "hatchback", label: "Хэтчбек" },
                      { value: "wagon", label: "Универсал" },
                      { value: "coupe", label: "Купе" },
                      { value: "cabriolet", label: "Кабриолет" },
                    ]}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Тип двигателя</span>
                  <SimpleFilterPicker
                    value={sellerExtras.engineType}
                    placeholder="Выберите тип двигателя"
                    onChange={(value) => setSellerExtras((prev) => ({ ...prev, engineType: value }))}
                    options={[
                      { value: "petrol", label: "Бензин" },
                      { value: "diesel", label: "Дизель" },
                      { value: "electric", label: "Электро" },
                    ]}
                  />
                </div>
                <div className="search-cell">
                  <span className="search-cell-label">Тип привода</span>
                  <SimpleFilterPicker
                    value={sellerExtras.driveType}
                    placeholder="Выберите тип привода"
                    onChange={(value) => setSellerExtras((prev) => ({ ...prev, driveType: value }))}
                    options={[
                      { value: "fwd", label: "Передний" },
                      { value: "rwd", label: "Задний" },
                      { value: "awd", label: "Полный" },
                    ]}
                  />
                </div>
              </div>
            </div>
              {showSellerAdvancedFilters && (
                <div className="advanced-filters">
                  {featureSections.length === 0 && (
                    <p className="search-panel-hint">Список опций не загружен — проверьте, что backend доступен.</p>
                  )}
                  <div className="feature-accordion">
                    {featureSections.map((section) => (
                      <div key={section.title} className="feature-row">
                        <button
                          type="button"
                          className="feature-row-btn"
                          onClick={() =>
                            setOpenedSellerSection((prev) => (prev === section.title ? null : section.title))
                          }
                        >
                          <span>{section.title}</span>
                          <span className={`feature-chevron ${openedSellerSection === section.title ? "open" : ""}`}>
                            ▾
                          </span>
                        </button>
                        {openedSellerSection === section.title && (
                          <div className="feature-row-content">
                            {section.items.map((item) => (
                              <button
                                key={item}
                                type="button"
                                className={`feature-option-chip ${sellerExtras.selectedFeatures.includes(item) ? "active" : ""}`}
                                onClick={() =>
                                  setSellerExtras((prev) => ({
                                    ...prev,
                                    selectedFeatures: prev.selectedFeatures.includes(item)
                                      ? prev.selectedFeatures.filter((feature) => feature !== item)
                                      : [...prev.selectedFeatures, item],
                                  }))
                                }
                              >
                                {item}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            {mode === "seller" && currentUser && editingId === null && (
              <div className="seller-photos card-inset">
                <h3 className="seller-photos-title">Фотографии нового объявления</h3>
                <p className="seller-photos-hint">
                  Добавьте фото до публикации: до 10 файлов, JPEG, PNG или WebP, до 8 МБ каждый.
                </p>
                <div className="seller-photos-toolbar">
                  <input
                    ref={createPhotoInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                    multiple
                    disabled={photoBusy}
                    onChange={handleCreatePhotoSelection}
                    className="seller-photos-file-native"
                    aria-label="Файлы изображений нового объявления"
                  />
                  <button
                    type="button"
                    className="secondary seller-photos-file-btn"
                    disabled={photoBusy}
                    onClick={() => createPhotoInputRef.current?.click()}
                  >
                    Выбрать фото
                  </button>
                  {createPhotoFiles.length > 0 && (
                    <span className="seller-photos-count">Выбрано: {createPhotoFiles.length}</span>
                  )}
                </div>
              </div>
            )}
            {mode === "seller" && currentUser && editingId != null && photoCarIdResolved != null && (
              <div className="seller-photos card-inset">
                <h3 className="seller-photos-title">Фотографии объявления</h3>
                <p className="seller-photos-hint">
                  До 10 файлов, JPEG, PNG или WebP (не HEIC с iPhone), до 8 МБ каждый.
                  {editingId != null ? " Режим редактирования." : " Объявление только что сохранено — можно добавить снимки."}
                </p>
                {photoBanner && (
                  <p
                    className={photoBanner.kind === "ok" ? "message success seller-photos-banner" : "message error seller-photos-banner"}
                    role="status"
                  >
                    {photoBanner.text}
                  </p>
                )}
                <div className="seller-photos-toolbar">
                  <input
                    ref={sellerPhotoInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                    multiple
                    disabled={photoBusy}
                    onChange={(e) => void handleSellerPhotoInput(e)}
                    className="seller-photos-file-native"
                    aria-label="Файлы изображений для объявления"
                  />
                  <button
                    type="button"
                    className="secondary seller-photos-file-btn"
                    disabled={photoBusy}
                    onClick={() => sellerPhotoInputRef.current?.click()}
                  >
                    {photoBusy ? "Загрузка…" : "Выбрать фото"}
                  </button>
                </div>
                {carForPhotos?.photos && carForPhotos.photos.length > 0 && photoCarIdResolved != null && (
                  <ul className="seller-photos-grid">
                    {carForPhotos.photos.map((ph) => (
                      <li key={ph.id} className="seller-photos-item">
                        <img src={mediaUrl(ph.url)} alt="" className="seller-photos-thumb" />
                        <button
                          type="button"
                          className="danger seller-photos-remove"
                          onClick={() => void deleteSellerPhoto(photoCarIdResolved, ph.id)}
                        >
                          Удалить
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
              <div className="search-actions seller-create-actions">
                <button
                  type="button"
                  className="secondary"
                  onClick={() => setShowSellerAdvancedFilters((prev) => !prev)}
                >
                  {showSellerAdvancedFilters ? "Скрыть параметры" : "Все параметры"}
                </button>
                <button type="button" className="secondary" onClick={() => cancelSellerEdit()}>
                  Сбросить
                </button>
                <button type="submit" className="show-results-btn">
                  {editingId ? "Сохранить объявление" : "Добавить объявление"}
                </button>
              </div>
          </form>
          </div>

          {editingId === null &&
            (currentUser?.accountType === "dealership" || currentUser?.accountType === "admin") && (
            <div className="dealership-bulk card-inset">
              <h3 className="dealership-bulk-title">Несколько автомобилей сразу</h3>
              <div className="dealership-bulk-toolbar">
                <button type="button" className="secondary" onClick={addBulkRow} disabled={bulkRows.length >= MAX_DEALERSHIP_BULK_ROWS}>
                  + Добавить позицию
                </button>
                <button type="button" className="secondary" onClick={appendBulkRowFromMainForm} disabled={bulkRows.length >= MAX_DEALERSHIP_BULK_ROWS}>
                  Дублировать из формы выше
                </button>
                <div className="bulk-currency-inline">
                  <span className="bulk-currency-label">Валюта</span>
                  <SimpleFilterPicker
                    compact
                    value={bulkPriceCurrency}
                    placeholder="USD"
                    options={LISTING_CURRENCY_OPTIONS}
                    onChange={(value) => setBulkPriceCurrency(value as ListingCurrency)}
                  />
                </div>
                <button
                  type="button"
                  className="show-results-btn"
                  disabled={bulkBusy || bulkRows.length === 0}
                  onClick={() => void submitDealershipBulk()}
                >
                  {bulkBusy ? "Отправка…" : `Опубликовать все (${bulkRows.length})`}
                </button>
              </div>

              <div className="bulk-car-list">
                {bulkRows.map((row, idx) => (
                  <div key={row.id} className="bulk-car-card">
                    <div className="bulk-car-card-head">
                      <span className="bulk-car-card-title">
                        Позиция {idx + 1}
                        {row.brand.trim() || row.model.trim() ? (
                          <span className="bulk-car-card-summary">
                            {" "}
                            — {row.brand.trim()} {row.model.trim()}
                            {row.year.trim() ? `, ${row.year}` : ""}
                          </span>
                        ) : null}
                      </span>
                      {bulkRows.length > 1 && (
                        <button type="button" className="secondary bulk-car-remove" onClick={() => removeBulkRow(row.id)}>
                          Удалить позицию
                        </button>
                      )}
                    </div>
                    <div className="search-table seller-create-table">
                      <div className="search-grid">
                        <div className="search-cell">
                          <span className="search-cell-label">Марка</span>
                          <input
                            placeholder="Марка"
                            value={row.brand}
                            onChange={(e) => updateBulkRow(row.id, { brand: e.target.value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Модель</span>
                          <input
                            placeholder="Например, Camry"
                            value={row.model}
                            onChange={(e) => updateBulkRow(row.id, { model: e.target.value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Год</span>
                          <input
                            type="number"
                            min={MIN_CAR_MODEL_YEAR}
                            max={MAX_CAR_MODEL_YEAR}
                            placeholder="Год"
                            value={row.year}
                            onChange={(e) => updateBulkRow(row.id, { year: normalizeYearInput(e.target.value) })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Цена</span>
                          <input
                            type="number"
                            min={1}
                            max={MAX_CAR_PRICE}
                            step="any"
                            placeholder="Цена"
                            value={row.price}
                            onChange={(e) => updateBulkRow(row.id, { price: e.target.value })}
                          />
                        </div>
                      </div>
                      <div className="search-grid">
                        <div className="search-cell">
                          <span className="search-cell-label">Город</span>
                          <input
                            placeholder="Например, Гродно"
                            value={row.city}
                            onChange={(e) => updateBulkRow(row.id, { city: e.target.value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Объём двигателя, л</span>
                          <input
                            type="number"
                            min={0.1}
                            max={20}
                            step={0.1}
                            placeholder="Например 2.0"
                            value={row.engineVolume}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                engineVolume: normalizeCappedNumberInput(e.target.value, 20, { allowDecimal: true }),
                              })
                            }
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Пробег, км</span>
                          <input
                            type="number"
                            min={0}
                            max={2000000}
                            step={1}
                            placeholder="Например 37000"
                            value={row.mileage}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                mileage: normalizeCappedNumberInput(e.target.value, 2000000),
                              })
                            }
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Мощность, л.с.</span>
                          <input
                            type="number"
                            min={1}
                            max={1500}
                            step={1}
                            placeholder="Мощность"
                            value={row.powerHp}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                powerHp: normalizeCappedNumberInput(e.target.value, 1500),
                              })
                            }
                          />
                        </div>
                      </div>
                      <div className="search-grid">
                        <div className="search-cell">
                          <span className="search-cell-label">Расход, л/100км (по городу)</span>
                          <input
                            type="number"
                            min={0.1}
                            max={100}
                            step={0.1}
                            placeholder="По городу"
                            value={row.fuelConsumptionCity}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                fuelConsumptionCity: normalizeCappedNumberInput(e.target.value, 100, {
                                  allowDecimal: true,
                                }),
                              })
                            }
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Расход, л/100км (по трассе)</span>
                          <input
                            type="number"
                            min={0.1}
                            max={100}
                            step={0.1}
                            placeholder="По трассе"
                            value={row.fuelConsumptionHighway}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                fuelConsumptionHighway: normalizeCappedNumberInput(e.target.value, 100, {
                                  allowDecimal: true,
                                }),
                              })
                            }
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Расход, л/100км (смешанный)</span>
                          <input
                            type="number"
                            min={0.1}
                            max={100}
                            step={0.1}
                            placeholder="Смешанный"
                            value={row.fuelConsumptionMixed}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                fuelConsumptionMixed: normalizeCappedNumberInput(e.target.value, 100, {
                                  allowDecimal: true,
                                }),
                              })
                            }
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">К-во мест</span>
                          <input
                            type="number"
                            min={1}
                            max={9}
                            step={1}
                            placeholder="Например 5"
                            value={row.seats}
                            onChange={(e) =>
                              updateBulkRow(row.id, {
                                seats: normalizeCappedNumberInput(e.target.value, 9),
                              })
                            }
                          />
                        </div>
                      </div>
                      <div className="search-grid search-grid--3">
                        <div className="search-cell">
                          <span className="search-cell-label">Цвет кузова</span>
                          <SimpleFilterPicker
                            value={row.color}
                            placeholder="Выберите цвет кузова"
                            options={bulkRowBodyColorOptions(row)}
                            onChange={(value) => updateBulkRow(row.id, { color: value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Цвет салона</span>
                          <SimpleFilterPicker
                            value={row.interiorColor}
                            placeholder="Выберите цвет салона"
                            options={bulkRowInteriorColorOptions(row)}
                            onChange={(value) => updateBulkRow(row.id, { interiorColor: value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Материал салона</span>
                          <SimpleFilterPicker
                            value={row.interiorMaterial}
                            placeholder="Выберите материал"
                            options={bulkRowMaterialOptions(row)}
                            onChange={(value) => updateBulkRow(row.id, { interiorMaterial: value })}
                          />
                        </div>
                      </div>
                      <div className="quick-filters-grid">
                        <div className="search-cell">
                          <span className="search-cell-label">Коробка передач</span>
                          <SimpleFilterPicker
                            value={row.transmission}
                            placeholder="Выберите коробку"
                            options={[
                              { value: "auto", label: "Автомат" },
                              { value: "manual", label: "Механика" },
                              { value: "robot", label: "Робот" },
                            ]}
                            onChange={(value) => updateBulkRow(row.id, { transmission: value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Тип кузова</span>
                          <SimpleFilterPicker
                            value={row.bodyType}
                            placeholder="Выберите тип кузова"
                            options={[
                              { value: "sedan", label: "Седан" },
                              { value: "suv", label: "SUV" },
                              { value: "hatchback", label: "Хэтчбек" },
                              { value: "wagon", label: "Универсал" },
                              { value: "coupe", label: "Купе" },
                              { value: "cabriolet", label: "Кабриолет" },
                            ]}
                            onChange={(value) => updateBulkRow(row.id, { bodyType: value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Тип двигателя</span>
                          <SimpleFilterPicker
                            value={row.engineType}
                            placeholder="Выберите тип двигателя"
                            options={[
                              { value: "petrol", label: "Бензин" },
                              { value: "diesel", label: "Дизель" },
                              { value: "electric", label: "Электро" },
                            ]}
                            onChange={(value) => updateBulkRow(row.id, { engineType: value })}
                          />
                        </div>
                        <div className="search-cell">
                          <span className="search-cell-label">Тип привода</span>
                          <SimpleFilterPicker
                            value={row.driveType}
                            placeholder="Выберите тип привода"
                            options={[
                              { value: "fwd", label: "Передний" },
                              { value: "rwd", label: "Задний" },
                              { value: "awd", label: "Полный" },
                            ]}
                            onChange={(value) => updateBulkRow(row.id, { driveType: value })}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {bulkMessage && <p className="message success">{bulkMessage}</p>}
              {bulkError && <p className="message error">{bulkError}</p>}
            </div>
          )}

          {editingId === null && message && <p className="message success">{message}</p>}
          {editingId === null && error && <p className="message error">{error}</p>}
        </section>
      )}

      <section className="card listings-catalog">
        <div className="listings-catalog-head">
          <h3 className="listings-catalog-title">
            {mode === "buyer"
              ? "Найденные объявления"
              : currentUser?.accountType === "admin"
                ? "Все объявления"
                : "Мои объявления"}
          </h3>
          {mode === "buyer" && <ListingSortPicker value={buyerSort} onChange={setBuyerSort} />}
        </div>
        <div className="listings-panel">
          {carsLoadStatus === "loading" && (
            <div className="table-empty-state listings-empty-pad">
              <p className="table-empty-title">Загрузка объявлений…</p>
              <p className="table-empty-hint">Подождите, запрашиваем список с сервера.</p>
            </div>
          )}
          {carsLoadStatus === "error" && (
            <div className="table-empty-state listings-empty-pad">
              <p className="table-empty-title">Не удалось загрузить объявления</p>
              <p className="table-empty-hint message error">{error || "Ошибка сети"}</p>
              <p className="table-empty-hint">
                Проверьте доступность backend по адресу {API_BASE.replace(/\/api\/?$/, "")}.
              </p>
              <button type="button" className="secondary mt8" onClick={() => void loadCars()}>
                Повторить загрузку
              </button>
            </div>
          )}
          {carsLoadStatus === "ok" && cars.length === 0 && mode === "buyer" && (
            <div className="table-empty-state listings-empty-pad">
              <p className="table-empty-title">Объявлений пока нет</p>
              <p className="table-empty-hint">
                В ответе API нет машин. Запустите приложение с DataInitializer или добавьте объявление в режиме
                продавца.
              </p>
            </div>
          )}
          {carsLoadStatus === "ok" && cars.length > 0 && listingsToShow.length === 0 && mode === "buyer" && (
            <div className="table-empty-state listings-empty-pad">
              <p className="table-empty-title">По выбранным фильтрам ничего не найдено</p>
              <p className="table-empty-hint">Попробуйте ослабить условия поиска.</p>
              <button type="button" className="secondary mt8" onClick={resetBuyerFilters}>
                Сбросить фильтры
              </button>
            </div>
          )}
          {carsLoadStatus === "ok" && mode === "seller" && listingsToShow.length === 0 && (
            <div className="table-empty-state listings-empty-pad">
              <p className="table-empty-title">
                {currentUser?.accountType === "admin"
                  ? "В каталоге пока нет объявлений"
                  : "У вас пока нет объявлений"}
              </p>
              <p className="table-empty-hint">
                {currentUser?.accountType === "admin"
                  ? "Добавьте авто через форму выше (можно указать владельца) или импортируйте пакетом для автосалона."
                  : `Добавьте первое авто через форму выше${
                      currentUser?.accountType === "dealership"
                        ? " или несколько — через таблицу пакетного добавления."
                        : "."
                    }`}
              </p>
            </div>
          )}
          {carsLoadStatus === "ok" && listingsToShow.length > 0 && (
            <>
            <div ref={listingsTopRef} />
            <div className="listing-cards" role="list">
              {listingsPageItems.map((car) => {
                const isFav = favoriteCarIds.has(car.id);
                const vatStyle = listingShowVatStyle(car);
                return (
                  <article
                    key={car.id}
                    role="listitem"
                    className={`listing-card ${mode === "buyer" ? "listing-card--buyer" : "listing-card--seller"}`}
                    onClick={mode === "buyer" ? () => setSelectedCar(car) : undefined}
                    onKeyDown={
                      mode === "buyer"
                        ? (ke) => {
                            if (ke.key === "Enter" || ke.key === " ") {
                              ke.preventDefault();
                              setSelectedCar(car);
                            }
                          }
                        : undefined
                    }
                    tabIndex={mode === "buyer" ? 0 : undefined}
                  >
                    <div className="listing-card__media">
                      {car.photos?.[0] ? (
                        <img
                          src={mediaUrl(car.photos[0].url)}
                          alt=""
                          className="listing-card__img"
                        />
                      ) : (
                        <span className="listing-card__img-placeholder">Нет фото</span>
                      )}
                    </div>
                    <div className="listing-card__body">
                      <div className="listing-card__head">
                        <div className="listing-card__title-wrap">
                          <h4 className="listing-card__title">
                            {car.brand} {car.model}
                          </h4>
                          <div className="listing-card__badges">
                            {listingShowTopChip(car) && (
                              <span className="listing-chip listing-chip--top">ТОП</span>
                            )}
                            <span className="listing-chip listing-chip--vin">VIN</span>
                          </div>
                        </div>
                        <div className="listing-card__specs">
                          <span className="listing-card__year">{car.year} г.</span>
                          <span className="listing-card__tech">{listingBodyWithDoors(car)}</span>
                          <span className="listing-card__mileage">{mileageLabel(car)}</span>
                        </div>
                      </div>
                      <p className="listing-card__snippet">{listingSnippetText(car)}</p>
                      <p className="listing-card__loc">
                        {car.city?.trim() || "Город не указан"} · {listingPublishedPhrase(car)}
                      </p>
                    </div>
                    <div className="listing-card__aside">
                      <div className="listing-card__price-row">
                        {vatStyle ? (
                          <div className="listing-card__price-vat">
                            <PriceDualBlock price={car.price} currencyCode={car.priceCurrency} vat />
                          </div>
                        ) : (
                          <div className="listing-card__price-plain">
                            <PriceDualBlock price={car.price} currencyCode={car.priceCurrency} />
                          </div>
                        )}
                        {mode === "buyer" && (
                          <button
                            type="button"
                            className={`listing-card__fav ${isFav ? "listing-card__fav--on" : ""}`}
                            aria-label={isFav ? "Убрать из избранного" : "В избранное"}
                            aria-pressed={isFav}
                            onClick={(e) => toggleListingFavorite(car.id, e)}
                          >
                            {isFav ? "♥" : "♡"}
                          </button>
                        )}
                      </div>
                      {mode === "buyer" && (
                        <p className="listing-card__leasing">{listingLeasingHint(car)}</p>
                      )}
                      <p className="listing-card__seller">{listingSellerLine(car)}</p>
                      {mode === "seller" && (
                        <div className="listing-card__actions" onClick={(e) => e.stopPropagation()}>
                          <button type="button" className="secondary" onClick={() => startEdit(car)}>
                            Изменить
                          </button>
                          <button type="button" className="danger" onClick={() => void deleteCar(car.id)}>
                            Удалить
                          </button>
                        </div>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
            {listingsTotalPages > 1 && (
              <div className="listings-pagination" aria-label="Пагинация объявлений">
                <button
                  type="button"
                  className="secondary"
                  onClick={goToPrevListingsPage}
                  disabled={listingsPage === 1}
                >
                  Назад
                </button>
                <span className="listings-pagination__status">
                  Страница {listingsPage} из {listingsTotalPages}
                </span>
                <button
                  type="button"
                  className="secondary"
                  onClick={goToNextListingsPage}
                  disabled={listingsPage === listingsTotalPages}
                >
                  Вперёд
                </button>
              </div>
            )}
            <div ref={listingsBottomRef} />
            </>
          )}
        </div>
      </section>
        </>
      )}
    </>
  );
}

export default function App() {
  const [, setTab] = useState<Tab>("cars");
  const [mode, setMode] = useState<UserMode>("buyer");
  const [accountOpen, setAccountOpen] = useState(false);
  const [accountView, setAccountView] = useState<"login" | "register-form">("login");
  const [registerKind, setRegisterKind] = useState<AccountKind>("person");
  const [currentUser, setCurrentUser] = useState<AuthSession | null>(() => {
    try {
      const raw = localStorage.getItem("autosalon_session");
      return raw ? (JSON.parse(raw) as AuthSession) : null;
    } catch {
      return null;
    }
  });
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [registerForm, setRegisterForm] = useState({
    personName: "",
    companyName: "",
    phone: "",
    address: "",
    password: "",
    confirmPassword: "",
  });
  const [authError, setAuthError] = useState("");
  const [allCars, setAllCars] = useState<Car[]>([]);
  const [favoriteCarIds, setFavoriteCarIds] = useState<Set<number>>(() => loadFavoriteCarIds());
  const [favoritesOpen, setFavoritesOpen] = useState(false);
  const [favoritesCatalog, setFavoritesCatalog] = useState<Car[] | null>(null);
  const [favoritesCatalogStatus, setFavoritesCatalogStatus] = useState<"idle" | "loading" | "ok" | "error">(
    "idle"
  );
  const favoritesWrapRef = useRef<HTMLDivElement>(null);
  const buyerOpenCarNonceRef = useRef(0);
  const [buyerOpenCarRequest, setBuyerOpenCarRequest] = useState<{ id: number; nonce: number } | null>(null);
  const [carsListLoadStatus, setCarsListLoadStatus] = useState<"loading" | "ok" | "error">("loading");
  const [selectedHeroBrand, setSelectedHeroBrand] = useState("");
  const [heroShowAllBrands, setHeroShowAllBrands] = useState(false);
  const [resetToListSignal, setResetToListSignal] = useState(0);
  const [heroModelNavigate, setHeroModelNavigate] = useState<HeroModelNavigate | null>(null);
  const [buyerCarDetailOpen, setBuyerCarDetailOpen] = useState(false);
  const profileRefreshRef = useRef(false);
  const [adminUsers, setAdminUsers] = useState<AdminUserRow[]>([]);
  const [adminDealerships, setAdminDealerships] = useState<Dealership[]>([]);
  const [adminPanelStatus, setAdminPanelStatus] = useState<"idle" | "loading" | "ok" | "error">("idle");
  const [adminPanelMessage, setAdminPanelMessage] = useState("");
  const [adminPanelBusy, setAdminPanelBusy] = useState(false);
  const [adminPwdDraft, setAdminPwdDraft] = useState<Record<number, string>>({});
  const adminPeopleRows = useMemo(
    () => adminUsers.filter((row) => row.accountType.trim().toLowerCase() !== "dealership"),
    [adminUsers]
  );
  const adminDealershipUserByDealershipId = useMemo(() => {
    const dealershipUsers = adminUsers.filter(
      (row) => row.accountType.trim().toLowerCase() === "dealership"
    );
    const byUsername = new Map<string, AdminUserRow>();
    const byCompanyName = new Map<string, AdminUserRow>();
    for (const row of dealershipUsers) {
      if (row.username?.trim()) {
        byUsername.set(row.username.trim(), row);
      }
      const company = row.companyName?.trim().toLowerCase();
      if (company) {
        byCompanyName.set(company, row);
      }
    }
    const linked = new Map<number, AdminUserRow>();
    for (const d of adminDealerships) {
      let row: AdminUserRow | undefined;
      if (d.phone?.trim()) {
        row = byUsername.get(d.phone.trim());
      }
      if (!row && d.name?.trim()) {
        row = byCompanyName.get(d.name.trim().toLowerCase());
      }
      if (row) {
        linked.set(d.id, row);
      }
    }
    return linked;
  }, [adminUsers, adminDealerships]);

  const onFavoriteToggle = useCallback((carId: number) => {
    setFavoriteCarIds((prev) => {
      const next = new Set(prev);
      if (next.has(carId)) next.delete(carId);
      else next.add(carId);
      persistFavoriteCarIds(next);
      return next;
    });
  }, []);

  const onBuyerOpenCarRequestHandled = useCallback(() => {
    setBuyerOpenCarRequest(null);
  }, []);

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === FAVORITE_CAR_IDS_KEY) {
        setFavoriteCarIds(loadFavoriteCarIds());
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  useEffect(() => {
    if (!favoritesOpen) {
      setFavoritesCatalog(null);
      setFavoritesCatalogStatus("idle");
      return;
    }
    let cancelled = false;
    setFavoritesCatalogStatus("loading");
    void api<Car[]>("/cars")
      .then((data) => {
        if (cancelled) return;
        setFavoritesCatalog(data || []);
        setFavoritesCatalogStatus("ok");
      })
      .catch(() => {
        if (cancelled) return;
        setFavoritesCatalog([]);
        setFavoritesCatalogStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [favoritesOpen]);

  useEffect(() => {
    if (!favoritesOpen) return;
    const onDoc = (e: MouseEvent) => {
      const el = favoritesWrapRef.current;
      if (el && !el.contains(e.target as Node)) {
        setFavoritesOpen(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setFavoritesOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDoc);
      document.removeEventListener("keydown", onKey);
    };
  }, [favoritesOpen]);

  const favoritesRows = useMemo(() => {
    const list = favoritesCatalog ?? [];
    const byId = new Map(list.map((c) => [c.id, c]));
    return [...favoriteCarIds].map((id) => ({ id, car: byId.get(id) }));
  }, [favoriteCarIds, favoritesCatalog]);

  const openCarFromFavorites = useCallback((car: Car) => {
    buyerOpenCarNonceRef.current += 1;
    setBuyerOpenCarRequest({ id: car.id, nonce: buyerOpenCarNonceRef.current });
    setFavoritesOpen(false);
    setAccountOpen(false);
    setMode("buyer");
    setTab("cars");
  }, []);

  const reloadAdminPanel = useCallback(async (token: string) => {
    const [users, dealers] = await Promise.all([
      apiAuth<AdminUserRow[]>("/admin/users", token),
      api<Dealership[]>("/dealerships"),
    ]);
    setAdminUsers(users ?? []);
    setAdminDealerships(dealers ?? []);
  }, []);

  useEffect(() => {
    if (!accountOpen || currentUser?.accountType !== "admin" || !currentUser.token) {
      if (!accountOpen) {
        setAdminPanelStatus("idle");
        setAdminPanelMessage("");
      }
      return;
    }
    let cancelled = false;
    setAdminPanelStatus("loading");
    setAdminPanelMessage("");
    void (async () => {
      try {
        await reloadAdminPanel(currentUser.token);
        if (!cancelled) setAdminPanelStatus("ok");
      } catch (e) {
        if (!cancelled) {
          setAdminPanelStatus("error");
          setAdminPanelMessage((e as Error).message);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [accountOpen, currentUser?.accountType, currentUser?.token, reloadAdminPanel]);

  useEffect(() => {
    if (!accountOpen) {
      profileRefreshRef.current = false;
      return;
    }
    const user = currentUser;
    if (!user?.token) return;
    if (profileRefreshRef.current) return;
    profileRefreshRef.current = true;
    let cancelled = false;
    void (async () => {
      try {
        const p = await api<AuthApiBody>("/auth/me", {
          headers: { Authorization: `Bearer ${user.token}` },
        });
        if (cancelled) return;
        setCurrentUser((prev) => {
          if (!prev || prev.token !== user.token) return prev;
          const merged = sessionFromAuthBody(p, prev.token);
          localStorage.setItem("autosalon_session", JSON.stringify(merged));
          return merged;
        });
      } catch {
        /* сессия могла истечь */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [accountOpen, currentUser?.token]);

  const openAccountChoice = () => {
    if (currentUser) {
      setMode("seller");
      setTab("cars");
    }
    setAccountOpen(true);
    setAccountView("login");
    setAuthError("");
  };

  const saveSession = (session: AuthSession | null) => {
    if (session) {
      localStorage.setItem("autosalon_session", JSON.stringify(session));
    } else {
      localStorage.removeItem("autosalon_session");
    }
    setCurrentUser(session);
  };

  const handleLogin = async () => {
    const username = loginForm.username.trim();
    const password = loginForm.password.trim();
    if (!username || !password) {
      setAuthError("Введите логин и пароль.");
      return;
    }
    try {
      const auth = await api<AuthApiBody>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      const token = auth.token;
      if (!token) {
        setAuthError("Сервер не вернул токен.");
        return;
      }
      saveSession(sessionFromAuthBody(auth, token));
      setAccountOpen(false);
      setMode("seller");
      setTab("cars");
      setAuthError("");
    } catch (err) {
      setAuthError((err as Error).message);
    }
  };

  const handleRegister = async () => {
    const personName = registerForm.personName.trim();
    const companyName = registerForm.companyName.trim();
    const phone = registerForm.phone.trim();
    const address = registerForm.address.trim();
    const password = registerForm.password.trim();
    const belarusPhonePattern = /^\+375\d{9}$/;
    if (!phone || !password) {
      setAuthError("Заполните все поля.");
      return;
    }
    if (!belarusPhonePattern.test(phone)) {
      setAuthError("Телефон должен быть строго в формате +375XXXXXXXXX.");
      return;
    }
    if (registerKind === "person" && !personName) {
      setAuthError("Для физ.лица укажите имя.");
      return;
    }
    if (registerKind === "dealership" && (!companyName || !address)) {
      setAuthError("Для юр.лица укажите название компании и адрес.");
      return;
    }
    if (password.length < 4) {
      setAuthError("Пароль должен быть не короче 4 символов.");
      return;
    }
    if (password !== registerForm.confirmPassword.trim()) {
      setAuthError("Пароли не совпадают.");
      return;
    }
    try {
      const auth = await api<AuthApiBody>("/auth/register", {
        method: "POST",
        body: JSON.stringify({
          personName: registerKind === "person" ? personName : null,
          companyName: registerKind === "dealership" ? companyName : null,
          phone,
          address: registerKind === "dealership" ? address : null,
          password,
          accountType: registerKind === "dealership" ? "DEALERSHIP" : "PERSON",
        }),
      });
      const token = auth.token;
      if (!token) {
        setAuthError("Сервер не вернул токен.");
        return;
      }
      saveSession(sessionFromAuthBody(auth, token));
      setAccountOpen(false);
      setMode("seller");
      setTab("cars");
      setAuthError("");
    } catch (err) {
      setAuthError((err as Error).message);
    }
  };

  const handleLogout = async () => {
    try {
      if (currentUser?.token) {
        await api<void>("/auth/logout", {
          method: "POST",
          headers: { Authorization: `Bearer ${currentUser.token}` },
        });
      }
    } catch {
      // Ignore logout errors and clear session locally.
    }
    saveSession(null);
    setMode("buyer");
    setAccountOpen(false);
  };

  const handleAdminDeleteUser = async (id: number) => {
    if (!currentUser?.token) return;
    const row = adminUsers.find((u) => u.id === id);
    if (!window.confirm(`Удалить пользователя ${row?.username ?? "без логина"} и все его объявления?`)) return;
    setAdminPanelBusy(true);
    setAdminPanelMessage("");
    try {
      await apiAuth(`/admin/users/${id}`, currentUser.token, { method: "DELETE" });
      await reloadAdminPanel(currentUser.token);
      setAdminPanelStatus("ok");
    } catch (e) {
      setAdminPanelMessage((e as Error).message);
    } finally {
      setAdminPanelBusy(false);
    }
  };

  const handleAdminSetPassword = async (id: number) => {
    if (!currentUser?.token) return;
    const pwd = (adminPwdDraft[id] ?? "").trim();
    if (pwd.length < 4) {
      setAdminPanelMessage("Пароль не короче 4 символов.");
      return;
    }
    setAdminPanelBusy(true);
    setAdminPanelMessage("");
    try {
      await apiAuth(`/admin/users/${id}/password`, currentUser.token, {
        method: "PUT",
        body: JSON.stringify({ newPassword: pwd }),
      });
      setAdminPwdDraft((prev) => ({ ...prev, [id]: "" }));
      setAdminPanelMessage("Пароль обновлён.");
    } catch (e) {
      setAdminPanelMessage((e as Error).message);
    } finally {
      setAdminPanelBusy(false);
    }
  };

  const handleAdminDeleteDealership = async (d: Dealership) => {
    if (!currentUser?.token) return;
    if (!window.confirm(`Удалить автосалон «${d.name}»? Операция необратима.`)) return;
    setAdminPanelBusy(true);
    setAdminPanelMessage("");
    try {
      await apiAuth(`/dealerships/${d.id}`, currentUser.token, { method: "DELETE" });
      await reloadAdminPanel(currentUser.token);
      setAdminPanelStatus("ok");
    } catch (e) {
      setAdminPanelMessage((e as Error).message);
    } finally {
      setAdminPanelBusy(false);
    }
  };

  const brandStats = useMemo(() => {
    const grouped = new Map<string, number>();
    allCars.forEach((c) => grouped.set(c.brand, (grouped.get(c.brand) || 0) + 1));
    return [...grouped.entries()].sort((a, b) => b[1] - a[1]);
  }, [allCars]);

  const heroVisibleBrandStats = useMemo(
    () => (heroShowAllBrands ? brandStats : brandStats.slice(0, 10)),
    [brandStats, heroShowAllBrands]
  );

  const selectedBrandModels = useMemo(() => {
    if (!selectedHeroBrand.trim()) return [];
    const grouped = new Map<string, number>();
    allCars
      .filter((car) => car.brand.trim().toLowerCase() === selectedHeroBrand.trim().toLowerCase())
      .forEach((car) => grouped.set(car.model, (grouped.get(car.model) || 0) + 1));
    return [...grouped.entries()].sort((a, b) => b[1] - a[1]);
  }, [allCars, selectedHeroBrand]);

  const handleHeroModelClick = (model: string) => {
    const brand = selectedHeroBrand.trim();
    const m = model.trim();
    if (!brand || !m) return;
    setAccountOpen(false);
    setMode("buyer");
    setTab("cars");
    setHeroModelNavigate({ nonce: Date.now(), brand, model: m });
  };

  return (
    <div className="page">
      <header className="topbar">
        <div className="topbar-inner">
          <button
            type="button"
            className="logo logo-btn"
            onClick={() => {
              setAccountOpen(false);
              setTab("cars");
              setMode("buyer");
              setSelectedHeroBrand("");
              setHeroModelNavigate(null);
              setResetToListSignal((prev) => prev + 1);
            }}
          >
            autosalon.by
          </button>
          <div className="topbar-spacer" />
          <div className="topbar-icons" aria-label="Действия">
            <div className="topbar-favorites-wrap" ref={favoritesWrapRef}>
              <button
                type="button"
                className={`icon-btn favorites-toolbar-btn${
                  favoriteCarIds.size > 0 ? " favorites-toolbar-btn--active" : ""
                }`}
                title="Избранное"
                aria-expanded={favoritesOpen}
                aria-haspopup="dialog"
                aria-controls="favorites-dropdown-panel"
                onClick={() => setFavoritesOpen((open) => !open)}
              >
                <span className="favorites-toolbar-icon" aria-hidden>
                  {favoriteCarIds.size > 0 ? "♥" : "♡"}
                </span>
                {favoriteCarIds.size > 0 && (
                  <span className="favorites-toolbar-badge">{favoriteCarIds.size}</span>
                )}
              </button>
              {favoritesOpen && (
                <div
                  id="favorites-dropdown-panel"
                  className="favorites-dropdown"
                  role="dialog"
                  aria-label="Избранные объявления"
                >
                  <div className="favorites-dropdown-head">
                    <span className="favorites-dropdown-title">Избранное</span>
                    <button
                      type="button"
                      className="favorites-dropdown-close"
                      aria-label="Закрыть"
                      onClick={() => setFavoritesOpen(false)}
                    >
                      ×
                    </button>
                  </div>
                  {favoriteCarIds.size === 0 && (
                    <p className="favorites-dropdown-empty">Пока нет объявлений. Нажмите ♡ в карточке в каталоге.</p>
                  )}
                  {favoriteCarIds.size > 0 && favoritesCatalogStatus === "loading" && (
                    <p className="favorites-dropdown-empty">Загрузка списка…</p>
                  )}
                  {favoriteCarIds.size > 0 && favoritesCatalogStatus === "error" && (
                    <p className="favorites-dropdown-empty message error">Не удалось загрузить каталог.</p>
                  )}
                  {favoriteCarIds.size > 0 && favoritesCatalogStatus === "ok" && (
                    <ul className="favorites-list">
                      {favoritesRows.map(({ id, car }) => (
                        <li key={id} className="favorites-list-item">
                          {car ? (
                            <>
                              <button
                                type="button"
                                className="favorites-item-main"
                                onClick={() => openCarFromFavorites(car)}
                              >
                                <span className="favorites-item-thumb-wrap">
                                  {car.photos?.[0] ? (
                                    <img
                                      src={mediaUrl(car.photos[0].url)}
                                      alt=""
                                      className="favorites-item-thumb"
                                    />
                                  ) : (
                                    <span className="favorites-item-no-photo">Нет фото</span>
                                  )}
                                </span>
                                <span className="favorites-item-text">
                                  <span className="favorites-item-title">
                                    {car.brand} {car.model}
                                  </span>
                                  <span className="favorites-item-meta-col">
                                    <span className="favorites-item-meta-year">{car.year} г.</span>
                                    <PriceDualBlock
                                      price={car.price}
                                      currencyCode={car.priceCurrency}
                                      className="price-dual--favorites"
                                    />
                                  </span>
                                </span>
                              </button>
                              <button
                                type="button"
                                className="favorites-item-unfav"
                                title="Убрать из избранного"
                                aria-label="Убрать из избранного"
                                onClick={() => onFavoriteToggle(id)}
                              >
                                ♥
                              </button>
                            </>
                          ) : (
                            <div className="favorites-item-missing">
                              <span className="favorites-item-missing-text">
                                Объявление больше не в каталоге
                              </span>
                              <button type="button" className="secondary" onClick={() => onFavoriteToggle(id)}>
                                Убрать
                              </button>
                            </div>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </div>
            <button
              type="button"
              className="icon-btn profile-btn"
              title="Профиль"
              onClick={openAccountChoice}
            >
              👤
            </button>
          </div>
          <button
            className="cta"
            onClick={() => {
              if (!currentUser) {
                openAccountChoice();
                return;
              }
              setAccountOpen(false);
              setMode("seller");
              setTab("cars");
            }}
          >
            Подать объявление
          </button>
        </div>
      </header>

      <main className="container">
        {accountOpen && (
          <section className={`card auth-card${!currentUser ? " auth-card--guest" : ""}`}>
            <div className="auth-panel">
              {currentUser ? (
                <>
                  <h3 className="auth-title">Профиль</h3>
                  <div className="auth-profile-grid">
                    {currentUser.accountType === "dealership" ? (
                      <>
                        <div className="auth-profile-item auth-profile-item-compact">
                          <span className="auth-profile-label">Название компании</span>
                          <strong className="auth-profile-value">
                            {currentUser.companyName?.trim() ||
                              currentUser.displayName?.trim() ||
                              "Не указано"}
                          </strong>
                        </div>
                        <div className="auth-profile-item">
                          <span className="auth-profile-label">Адрес</span>
                          <strong className="auth-profile-value">
                            {currentUser.address?.trim() || "Не указано"}
                          </strong>
                        </div>
                      </>
                    ) : currentUser.accountType === "admin" ? (
                      <div className="auth-profile-item auth-profile-item-compact">
                        <span className="auth-profile-label">Роль</span>
                        <strong className="auth-profile-value">
                          {currentUser.displayName?.trim() || "Администратор"}
                        </strong>
                      </div>
                    ) : (
                      <div className="auth-profile-item auth-profile-item-compact">
                        <span className="auth-profile-label">Имя</span>
                        <strong className="auth-profile-value">
                          {currentUser.personName?.trim() ||
                            currentUser.displayName?.trim() ||
                            "Не указано"}
                        </strong>
                      </div>
                    )}
                    <div className="auth-profile-item">
                      <span className="auth-profile-label">Телефон (логин)</span>
                      <strong className="auth-profile-value">{currentUser.username}</strong>
                    </div>
                    <div className="auth-profile-item">
                      <span className="auth-profile-label">Тип аккаунта</span>
                      <strong className="auth-profile-value">
                        {currentUser.accountType === "person"
                          ? "Физ.лицо"
                          : currentUser.accountType === "dealership"
                            ? "Автосалон"
                            : "Администратор"}
                      </strong>
                    </div>
                  </div>
                  <div className="auth-actions auth-actions--profile">
                    <button type="button" className="secondary" onClick={handleLogout}>
                      Выйти
                    </button>
                  </div>
                  {currentUser.accountType === "admin" && (
                    <div className="admin-panel">
                      <h4 className="admin-panel-title">Администрирование</h4>
                      {adminPanelStatus === "loading" && (
                        <span className="admin-panel-hint">Загрузка…</span>
                      )}
                      {adminPanelMessage && (
                        <p
                          className={
                            adminPanelMessage === "Пароль обновлён."
                              ? "message success"
                              : "message error"
                          }
                        >
                          {adminPanelMessage}
                        </p>
                      )}
                      {adminPanelStatus === "ok" && (
                        <>
                          <h5 className="admin-panel-section">Пользователи</h5>
                          <div className="admin-table-wrap">
                            <table className="admin-table">
                              <thead>
                                <tr>
                                  <th>Логин</th>
                                  <th>Тип</th>
                                  <th>Имя / компания</th>
                                  <th>Действия</th>
                                </tr>
                              </thead>
                              <tbody>
                                {adminPeopleRows.map((row) => {
                                  const isSelf = row.username === currentUser.username;
                                  return (
                                    <tr key={row.id}>
                                      <td>{row.username}</td>
                                      <td>{formatAdminAccountType(row.accountType)}</td>
                                      <td>{adminUserDisplayLabel(row)}</td>
                                      <td className="admin-table-actions">
                                        <input
                                          type="password"
                                          autoComplete="new-password"
                                          placeholder="Новый пароль"
                                          className="admin-pwd-input"
                                          value={adminPwdDraft[row.id] ?? ""}
                                          disabled={adminPanelBusy}
                                          onChange={(e) =>
                                            setAdminPwdDraft((prev) => ({
                                              ...prev,
                                              [row.id]: e.target.value,
                                            }))
                                          }
                                        />
                                        <button
                                          type="button"
                                          className="secondary"
                                          disabled={adminPanelBusy}
                                          onClick={() => void handleAdminSetPassword(row.id)}
                                        >
                                          Изменить
                                        </button>
                                        <button
                                          type="button"
                                          className="secondary"
                                          disabled={adminPanelBusy || isSelf}
                                          title={
                                            isSelf
                                              ? "Нельзя удалить текущую сессию"
                                              : "Удалить пользователя и объявления"
                                          }
                                          onClick={() => void handleAdminDeleteUser(row.id)}
                                        >
                                          Удалить
                                        </button>
                                      </td>
                                    </tr>
                                  );
                                })}
                              </tbody>
                            </table>
                          </div>
                          <h5 className="admin-panel-section">Автосалоны</h5>
                          <div className="admin-table-wrap">
                            <table className="admin-table">
                              <thead>
                                <tr>
                                  <th>Название</th>
                                  <th>Адрес</th>
                                  <th>Действия</th>
                                </tr>
                              </thead>
                              <tbody>
                                {adminDealerships.map((d) => {
                                  const linkedUser = adminDealershipUserByDealershipId.get(d.id);
                                  return (
                                  <tr key={d.id}>
                                    <td>{d.name}</td>
                                    <td>{d.address}</td>
                                    <td className="admin-table-actions">
                                      <input
                                        type="password"
                                        autoComplete="new-password"
                                        placeholder="Новый пароль"
                                        className="admin-pwd-input"
                                        value={linkedUser ? (adminPwdDraft[linkedUser.id] ?? "") : ""}
                                        disabled={adminPanelBusy || !linkedUser}
                                        onChange={(e) => {
                                          if (!linkedUser) return;
                                          setAdminPwdDraft((prev) => ({
                                            ...prev,
                                            [linkedUser.id]: e.target.value,
                                          }));
                                        }}
                                      />
                                      <button
                                        type="button"
                                        className="secondary"
                                        disabled={adminPanelBusy || !linkedUser}
                                        title={linkedUser ? "Изменить пароль автосалона" : "Не найден связанный аккаунт автосалона"}
                                        onClick={() => linkedUser && void handleAdminSetPassword(linkedUser.id)}
                                      >
                                        Изменить
                                      </button>
                                      <button
                                        type="button"
                                        className="secondary"
                                        disabled={adminPanelBusy}
                                        onClick={() => void handleAdminDeleteDealership(d)}
                                      >
                                        Удалить
                                      </button>
                                    </td>
                                  </tr>
                                )})}
                              </tbody>
                            </table>
                          </div>
                        </>
                      )}
                    </div>
                  )}
                </>
              ) : accountView === "login" && (
                <>
                  <h3 className="auth-title">Вход</h3>
                  <p className="auth-subtitle">Войдите, чтобы управлять объявлениями.</p>
                  <div className="auth-login-card">
                    <div className="auth-login-body">
                      <label className="auth-field-label">Логин</label>
                      <input
                        placeholder="Введите логин"
                        value={loginForm.username}
                        onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                      />
                      <label className="auth-field-label">Пароль</label>
                      <div className="auth-password-wrap">
                        <input
                          type={showLoginPassword ? "text" : "password"}
                          placeholder="Введите пароль"
                          value={loginForm.password}
                          onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                        />
                        <button
                          type="button"
                          className="auth-password-toggle"
                          onClick={() => setShowLoginPassword((prev) => !prev)}
                          aria-label={showLoginPassword ? "Скрыть пароль" : "Показать пароль"}
                          title={showLoginPassword ? "Скрыть пароль" : "Показать пароль"}
                        >
                          {showLoginPassword ? "🙈" : "👁"}
                        </button>
                      </div>
                      <button
                        type="button"
                        className="auth-submit-btn"
                        disabled={!loginForm.username.trim() || !loginForm.password.trim()}
                        onClick={handleLogin}
                      >
                        Войти
                      </button>
                    </div>
                  </div>
                  {authError && <p className="message error">{authError}</p>}
                  <p className="auth-guest-footer">
                    Нет аккаунта?
                    <button
                      type="button"
                      className="auth-link-btn auth-link-btn--inline"
                      onClick={() => {
                        setRegisterKind("person");
                        setAccountView("register-form");
                        setAuthError("");
                      }}
                    >
                      Регистрация
                    </button>
                  </p>
                </>
              )}

              {!currentUser && accountView === "register-form" && (
                <>
                  <h3 className="auth-title">Регистрация</h3>
                  <p className="auth-subtitle">Создайте аккаунт и публикуйте объявления.</p>
                  <div className="auth-login-card">
                    <div className="auth-login-tabs">
                      <button
                        type="button"
                        className={`auth-login-tab ${registerKind === "person" ? "active" : ""}`}
                        onClick={() => setRegisterKind("person")}
                      >
                        физ.лицо
                      </button>
                      <button
                        type="button"
                        className={`auth-login-tab ${registerKind === "dealership" ? "active" : ""}`}
                        onClick={() => setRegisterKind("dealership")}
                      >
                        юр.лицо
                      </button>
                    </div>
                    <div className="auth-login-body">
                      {registerKind === "person" ? (
                        <>
                          <label className="auth-field-label">Имя на кириллице</label>
                          <input
                            placeholder="Введите имя"
                            value={registerForm.personName}
                            onChange={(e) => setRegisterForm({ ...registerForm, personName: e.target.value })}
                          />
                        </>
                      ) : (
                        <>
                          <label className="auth-field-label">Название компании</label>
                          <input
                            placeholder="Введите название компании"
                            value={registerForm.companyName}
                            onChange={(e) => setRegisterForm({ ...registerForm, companyName: e.target.value })}
                          />
                        </>
                      )}
                      <label className="auth-field-label">Телефон</label>
                      <input
                        type="tel"
                        placeholder="+375XXXXXXXXX"
                        inputMode="numeric"
                        maxLength={13}
                        value={registerForm.phone}
                        onChange={(e) => setRegisterForm({ ...registerForm, phone: e.target.value })}
                      />
                      {registerKind === "dealership" && (
                        <>
                          <label className="auth-field-label">Адрес</label>
                          <input
                            placeholder="Введите адрес"
                            value={registerForm.address}
                            onChange={(e) => setRegisterForm({ ...registerForm, address: e.target.value })}
                          />
                        </>
                      )}
                      <label className="auth-field-label">Пароль</label>
                      <input
                        type="password"
                        placeholder="Введите пароль"
                        value={registerForm.password}
                        onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })}
                      />
                      <label className="auth-field-label">Подтвердите пароль</label>
                      <input
                        type="password"
                        placeholder="Повторите пароль"
                        value={registerForm.confirmPassword}
                        onChange={(e) => setRegisterForm({ ...registerForm, confirmPassword: e.target.value })}
                      />
                    </div>
                  </div>
                  {authError && <p className="message error">{authError}</p>}
                  <div className="auth-actions">
                    <button type="button" className="cta" onClick={handleRegister}>Зарегистрироваться</button>
                    <button type="button" className="secondary" onClick={() => setAccountView("login")}>Назад</button>
                  </div>
                </>
              )}
            </div>
          </section>
        )}
        <>
        {mode === "buyer" && !buyerCarDetailOpen && (!accountOpen || currentUser) && (
          <section className="hero">
            <h1 className="hero-count-title">
              {carsListLoadStatus === "loading" && "Загрузка каталога…"}
              {carsListLoadStatus === "error" && "Каталог временно недоступен"}
              {carsListLoadStatus === "ok" && `${allCars.length} объявлений о продаже авто`}
            </h1>
            <div className="hero-market-divider" aria-hidden />
            <div className="brands">
              {carsListLoadStatus === "ok" &&
                heroVisibleBrandStats.map(([brand, count]) => (
                  <button
                    type="button"
                    key={brand}
                    className={`brand-item ${selectedHeroBrand === brand ? "active" : ""}`}
                    onClick={() => {
                      setHeroModelNavigate(null);
                      setSelectedHeroBrand((prev) => (prev === brand ? "" : brand));
                      setMode("buyer");
                      setTab("cars");
                    }}
                  >
                    <span>{brand}</span>
                    <small>{count}</small>
                  </button>
                ))}
              {carsListLoadStatus === "loading" && (
                <p className="hero-brands-hint">Загрузка марок…</p>
              )}
              {carsListLoadStatus === "error" && (
                <p className="hero-brands-hint">Проверьте backend и настройки API</p>
              )}
            </div>
            {carsListLoadStatus === "ok" && (
              <div className="hero-brands-actions">
                <button
                  type="button"
                  className="hero-all-brands-btn"
                  onClick={() => {
                    setHeroShowAllBrands((prev) => !prev);
                  }}
                >
                  {heroShowAllBrands ? "Скрыть" : "Все марки"}
                </button>
              </div>
            )}
            {carsListLoadStatus === "ok" && selectedHeroBrand.trim() && (
              <div className="hero-models">
                <div className="hero-models-header">
                  <strong>Модели {selectedHeroBrand} в наличии</strong>
                </div>
                <div className="hero-models-grid">
                  {selectedBrandModels.map(([model, count]) => (
                    <button
                      key={model}
                      type="button"
                      className="model-item model-item--btn"
                      title={
                        count <= 1
                          ? "Открыть объявление"
                          : `Показать все объявления (${count})`
                      }
                      onClick={() => handleHeroModelClick(model)}
                    >
                      <span>{model}</span>
                      <small>{count}</small>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        {(!accountOpen || currentUser) && (
          <CarsTabWithDesign
            onCarsLoaded={setAllCars}
            onCarsLoadStatus={setCarsListLoadStatus}
            selectedBrandFromHero={selectedHeroBrand}
            resetToListSignal={resetToListSignal}
            heroModelNavigate={heroModelNavigate}
            onBuyerCarDetailOpen={setBuyerCarDetailOpen}
            mode={mode}
            currentUser={currentUser}
            favoriteCarIds={favoriteCarIds}
            onFavoriteToggle={onFavoriteToggle}
            buyerOpenCarRequest={buyerOpenCarRequest}
            onBuyerOpenCarRequestHandled={onBuyerOpenCarRequestHandled}
          />
        )}
        </>
      </main>
    </div>
  );
}
