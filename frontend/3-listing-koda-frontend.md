# 3 ЛИСТИНГ КОДА (глава «Клиент»)

Ниже приведены фрагменты реализации SPA-клиента (React, Vite): точка монтирования приложения, типы данных и обёртка над REST API, отображение связей **OneToMany** и **ManyToMany**, загрузка каталога с сервера, клиентская **фильтрация** и **CRUD** по объявлениям, фрагмент **адаптивной** вёрстки. Полный код интерфейса сосредоточен в `frontend/src/App.tsx` (монолитный модуль).

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

@media (max-width: 900px) {
  .listing-card {
    grid-template-columns: 1fr;
  }

  .listing-card__aside {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    padding-top: 12px;
  }

  .listing-card__head {
    flex-direction: column;
  }
}
```
