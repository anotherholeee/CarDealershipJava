# 3 ЛИСТИНГ КОДА (глава «Клиент»)

Ниже приведены фрагменты реализации SPA-клиента (React, Vite): точка монтирования приложения, типы данных и обёртка над REST API, отображение связей **OneToMany** и **ManyToMany**, загрузка каталога с сервера, клиентская **фильтрация** и **CRUD** по объявлениям, **сортировка** и справочники марок/моделей, **валидация и сбор DTO** для API, **карточка объявления**, **авторизация и админ-операции**, фрагменты **адаптивной** вёрстки (`index.css`). Полный код интерфейса сосредоточен в `frontend/src/App.tsx` (монолитный модуль). Дополнительно приведены фрагменты `package.json`, `index.html`, `vite.config.ts`, `tsconfig.json`.

---

## Файл `main.tsx`

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

---

## Файл `App.tsx` — типы сущностей и HTTP-клиент к API

```tsx
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
  priceCurrency?: string | null;
  sellerDisplayName?: string | null;
  sellerPhone?: string | null;
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

const API_BASE =
  import.meta.env.VITE_API_BASE_URL?.trim() || "http://localhost:8080/api";

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
```

---

## Файл `App.tsx` — вкладка «Связи»: OneToMany и ManyToMany

```tsx
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
                {d.name} (id={d.id})
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
```

---

## Файл `App.tsx` — загрузка каталога, фильтрация, CRUD по объявлениям

```tsx
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
        setMessage("Объявление добавлено. Ниже можно прикрепить фотографии.");
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
```

---

## Файл `index.css` — адаптивная сетка карточки объявления

```css
@media (max-width: 900px) {
  .listing-card {
    grid-template-columns: 1fr;
  }

  .listing-card__aside {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    padding-top: 12px;
    align-items: stretch;
    text-align: left;
  }

  .listing-card__price-row {
    justify-content: space-between;
  }

  .listing-card__specs {
    text-align: left;
    max-width: none;
  }

  .listing-card__head {
    flex-direction: column;
  }
}
```

---

## Файл `App.tsx` — авторизованные запросы и сессия

```tsx
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

type AuthSession = {
  token: string;
  username: string;
  accountType: "person" | "dealership" | "admin";
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

function sessionFromAuthBody(body: AuthApiBody, tokenFallback: string): AuthSession {
  const token = body.token && body.token.length > 0 ? body.token : tokenFallback;
  let accountType: AuthSession["accountType"] = "person";
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
```

---

## Файл `App.tsx` — начальное состояние фильтров, сортировки и типы для API создания авто

```tsx
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

const MAX_CAR_PRICE = 1_000_000;
const MIN_CAR_MODEL_YEAR = 1886;
const MAX_CAR_MODEL_YEAR = 2026;
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
  ownerUserId: string;
};

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
```

---

## Файл `App.tsx` — валидация полей формы и формирование JSON-тела для `POST/PUT /cars`

```tsx
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
    return { ok: false, message: `Заполните обязательные поля: ${missing.join(", ")}.` };
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
```

---

## Файл `App.tsx` — сортировка отфильтрованного каталога и справочники марок/моделей

```tsx
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

  const uniqueBrands = useMemo(() => {
    const set = new Set<string>();
    cars.forEach((c) => {
      if (c.brand?.trim()) set.add(c.brand.trim());
    });
    return [...set].sort((a, b) => a.localeCompare(b, "ru"));
  }, [cars]);

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
```

---

## Файл `App.tsx` — компонент выбора сортировки каталога

```tsx
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
```

---

## Файл `App.tsx` — детальная карточка объявления (галерея, цена, опции ManyToMany на UI)

```tsx
function CarDetailsPage({ car, onBack, isFavorite, onFavoriteToggle }: CarDetailsProps) {
  const [activePhoto, setActivePhoto] = useState(0);
  const [callSellerNotice, setCallSellerNotice] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
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

  const photos = galleryUrls;

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
      <p className="details-subtitle">{car.color}, в наличии</p>
      <p className="details-published">Опубликовано: {formatPublishedRelative(car.publishedAt)}</p>

      <div className="details-layout">
        <div className="details-gallery">
          <img className="details-main-image" src={photos[activePhoto]} alt={`${car.brand} ${car.model}`} />
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
          </p>
          <button type="button" className="show-results-btn details-primary-btn" onClick={() => void handleCallSeller()}>
            Позвонить продавцу
          </button>
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
```

---

## Файл `App.tsx` — фрагмент корневого компонента: вход, регистрация, админ-CRUD по пользователям и салонам

```tsx
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
    if (!window.confirm(`Удалить пользователя ${row?.username ?? id} и все его объявления?`)) return;
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
    if (!window.confirm(`Удалить автосалон «${d.name}» (id ${d.id})? Операция необратима.`)) return;
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
```

---

## Файл `index.css` — тема, сетка карточки объявления, адаптив сортировки и узкий экран

```css
:root {
  color-scheme: dark;
  --royal: #2a56f2;
  --royal-mid: #2346d8;
  --royal-deep: #1a3688;
  --aqua: #9dfecb;
  --aqua-muted: rgba(157, 254, 203, 0.72);
  --bg-base: #1a222e;
  --bg-raised: #232d3c;
  --bg-topbar: #222c3a;
  --text: #f6f2e9;
  --text-soft: #c4f0d4;
  --listing-card-bg: #12161d;
  --listing-card-edge: rgba(255, 255, 255, 0.07);
}

.listing-card {
  display: grid;
  grid-template-columns: minmax(200px, 260px) minmax(0, 1fr) minmax(188px, 248px);
  gap: 16px 18px;
  align-items: stretch;
  padding: 14px 16px;
  background: linear-gradient(160deg, #181e28 0%, var(--listing-card-bg) 55%, #0e1117 100%);
  border: 1px solid var(--listing-card-edge);
  border-radius: 12px;
}

.listing-card--buyer {
  cursor: pointer;
}

.listing-card--buyer:hover,
.listing-card--buyer:focus-visible {
  border-color: rgba(91, 159, 255, 0.45);
  outline: none;
}

@media (max-width: 520px) {
  .listing-sort-picker {
    width: 100%;
  }

  .listing-sort-trigger {
    min-width: 0;
    width: 100%;
  }

  .listing-sort-menu {
    left: 0;
    right: 0;
    min-width: 0;
  }
}
```

*(Медиазапрос `max-width: 900px` для `.listing-card` — в начале документа, отдельным фрагментом.)*

---

## Файл `package.json` — зависимости и скрипты SPA

```json
{
  "name": "frontend",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "lint": "eslint .",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^19.2.5",
    "react-dom": "^19.2.5"
  },
  "devDependencies": {
    "@eslint/js": "^10.0.1",
    "@types/node": "^24.12.2",
    "@types/react": "^19.2.14",
    "@types/react-dom": "^19.2.3",
    "@vitejs/plugin-react": "^6.0.1",
    "eslint": "^10.2.1",
    "eslint-plugin-react-hooks": "^7.1.1",
    "eslint-plugin-react-refresh": "^0.5.2",
    "globals": "^17.5.0",
    "typescript": "~6.0.2",
    "typescript-eslint": "^8.58.2",
    "vite": "^8.0.10"
  }
}
```

---

## Файл `index.html` — корневой HTML, viewport и точка входа модуля

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="theme-color" content="#1a222e" />
    <title>frontend</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

---

## Файл `vite.config.ts` — сборщик Vite и плагин React

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
})
```

---

## Файл `tsconfig.json` — проектные ссылки TypeScript

```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" },
    { "path": "./tsconfig.node.json" }
  ]
}
```

---

## Файл `App.tsx` — валюта объявления, блок цены, избранное в `localStorage`

```tsx
const RAW_BYN_PER_USD = Number(import.meta.env.VITE_BYN_PER_USD);
const BYN_PER_USD =
  Number.isFinite(RAW_BYN_PER_USD) && RAW_BYN_PER_USD > 0 ? RAW_BYN_PER_USD : 3.25;

type ListingCurrency = "USD" | "BYN";

function listingCurrencyLabel(code: string | null | undefined): ListingCurrency {
  const c = (code || "USD").trim().toUpperCase();
  return c === "BYN" ? "BYN" : "USD";
}

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
```

---

## Файл `App.tsx` — дата публикации и подсказка по лизингу для карточки

```tsx
function listingLeasingHint(car: Car): string {
  const { byn, usd } = carDualAmounts(car.price, car.priceCurrency);
  const monthlyByn = Math.max(120, Math.round(byn / 56));
  const monthlyUsd = Math.max(5, Math.round(usd / 56));
  return `Лизинг от ${monthlyByn.toLocaleString("ru-RU")} р. (≈ ${monthlyUsd.toLocaleString("ru-RU")} USD) в месяц`;
}

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
```

---

## Файл `App.tsx` — URL медиа с бэкенда, опции в каталоге, копирование в буфер

```tsx
type FeatureApiRow = { id: number; name: string; category?: string | null };

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
```

---

## Файл `App.tsx` — текстовые подписи для списка объявлений

```tsx
function listingSellerLine(car: Car): string {
  const name = car.sellerDisplayName?.trim();
  if (name) return name;
  if (car.sellerAccountType === "DEALERSHIP") return "Автосалон";
  if (car.sellerAccountType === "PERSON") return "Частный продавец";
  return "Продавец";
}

function listingSnippetText(car: Car): string {
  if (car.featureNames?.length) {
    return car.featureNames.slice(0, 4).join(", ");
  }
  const interior = car.interiorColor?.trim();
  return [car.color, interior ? `салон ${interior}` : null].filter(Boolean).join(", ") || "Подробности в карточке объявления.";
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
```

---

## Файл `App.tsx` — словари кодов КПП/ДВС/привода/кузова и нормализация ввода

```tsx
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
```

---

## Файл `App.tsx` — выпадающий список-фильтр (марка, КПП, валюта и т.д.)

```tsx
type PickerOption = {
  value: string;
  label: string;
};

type SimpleFilterPickerProps = {
  value: string;
  placeholder: string;
  options: PickerOption[];
  onChange: (value: string) => void;
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
```

---

## Файл `App.tsx` — загрузка фото (`multipart`) и пакетное создание объявлений салоном

```tsx
  const handleSellerPhotoInput = async (e: ChangeEvent<HTMLInputElement>) => {
    if (photoUploadLockRef.current || photoBusy) {
      e.target.value = "";
      return;
    }
    const id = photoCarIdResolved;
    const input = e.target;
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
      const n = Array.isArray(parsed) ? parsed.length : 0;
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
```

---

## Файл `index.css` — шапка сайта и панель «Избранное»

```css
.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
}

.topbar-spacer {
  flex: 1;
}

.topbar-icons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.favorites-toolbar-btn--active {
  color: #ff7a9a;
  border-color: rgba(255, 122, 154, 0.45);
  background: rgba(255, 122, 154, 0.12);
}

.favorites-toolbar-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--royal);
  color: var(--text);
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  text-align: center;
  pointer-events: none;
}

.favorites-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: min(380px, calc(100vw - 24px));
  max-height: min(440px, 72vh);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--bg-card-deep);
  border: 1px solid var(--border-line);
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
  z-index: 200;
}
```

---

## Файл `index.css` — адаптив панелей поиска и карточки деталей (`max-width: 980px`)

```css
@media (max-width: 980px) {
  .brands {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-models-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .seller-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .topbar-icons {
    display: none;
  }

  .search-grid {
    grid-template-columns: 1fr;
  }

  .search-grid .search-cell {
    border-right: none;
  }

  .quick-filters-grid {
    grid-template-columns: 1fr;
    border-top: 1px solid var(--line-grid);
  }

  .seller-create-grid {
    grid-template-columns: 1fr;
  }

  .advanced-grid,
  .advanced-grid.numbers {
    grid-template-columns: 1fr;
  }

  .details-layout {
    grid-template-columns: 1fr;
  }

  .details-title {
    font-size: clamp(20px, 5vw, 26px);
  }
}
```
