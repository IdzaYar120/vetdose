import { useLiveQuery } from "dexie-react-hooks"
import { useState } from "react"
import type { Product } from "../../domain/model"
import {
  getAllFavoriteProductIds,
  getSpeciesById,
  searchProducts,
  setFavorite,
} from "../../data/repositories"
import "./ProductSearchScreen.css"

interface ProductSearchScreenProps {
  speciesId: string
  onProductChosen: (product: Product) => void
  onClose: () => void
}

/**
 * Rendered as a full-screen overlay from `CalculateScreen`, not as a router
 * route: a real navigation to `/search/...` and back would unmount and
 * remount `CalculateScreen`, discarding its in-progress species/weight
 * selection (React Router has no equivalent of Android's back-stack-retained
 * ViewModel). Keeping this as a sibling element the parent shows/hides
 * avoids that entirely.
 */
export function ProductSearchScreen({ speciesId, onProductChosen, onClose }: ProductSearchScreenProps) {
  const [query, setQuery] = useState("")
  const [speciesFilterEnabled, setSpeciesFilterEnabled] = useState(true)

  const species = useLiveQuery(async () => await getSpeciesById(speciesId), [speciesId], null)
  const effectiveSpeciesId = speciesFilterEnabled ? speciesId : null
  const products = useLiveQuery(
    async () => await searchProducts(query.trim() === "" ? null : query, effectiveSpeciesId),
    [query, effectiveSpeciesId],
    [] as Product[],
  )
  const favoriteIds = useLiveQuery(getAllFavoriteProductIds, [], new Set<string>())

  const loaded = products !== undefined && favoriteIds !== undefined
  const favorites = (products ?? []).filter((p) => favoriteIds?.has(p.id))
  const others = (products ?? []).filter((p) => !favoriteIds?.has(p.id))
  const isEmpty = loaded && favorites.length === 0 && others.length === 0

  const toggleFavorite = (productId: string, isCurrentlyFavorite: boolean) => {
    void setFavorite(productId, !isCurrentlyFavorite)
  }

  return (
    <div className="search-screen-overlay">
      <header className="search-screen-overlay__header">
        <button type="button" className="app-header__back" onClick={onClose} aria-label="Назад">
          ←
        </button>
        <h1>Пошук препарату</h1>
      </header>
      <div className="search-screen">
        <input
          type="search"
          placeholder="Назва препарату або діюча речовина"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoFocus
        />

        {species != null && (
          <button
            type="button"
            className={`chip${speciesFilterEnabled ? " chip--selected" : ""}`}
            onClick={() => setSpeciesFilterEnabled((v) => !v)}
          >
            {speciesFilterEnabled ? `Вид: ${species.nameUk}` : "Усі види"}
          </button>
        )}

        {isEmpty ? (
          <div className="empty-state">Нічого не знайдено</div>
        ) : (
          <div className="search-screen__list">
            {favorites.length > 0 && (
              <>
                <p className="search-screen__section">Обрані</p>
                {favorites.map((product) => (
                  <ProductRow
                    key={`fav_${product.id}`}
                    product={product}
                    isFavorite={true}
                    onClick={() => onProductChosen(product)}
                    onFavoriteToggle={() => toggleFavorite(product.id, true)}
                  />
                ))}
              </>
            )}
            {others.length > 0 && (
              <>
                <p className="search-screen__section">Усі препарати</p>
                {others.map((product) => (
                  <ProductRow
                    key={`all_${product.id}`}
                    product={product}
                    isFavorite={false}
                    onClick={() => onProductChosen(product)}
                    onFavoriteToggle={() => toggleFavorite(product.id, false)}
                  />
                ))}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function ProductRow({
  product,
  isFavorite,
  onClick,
  onFavoriteToggle,
}: {
  product: Product
  isFavorite: boolean
  onClick: () => void
  onFavoriteToggle: () => void
}) {
  return (
    <div className="card search-screen__row">
      <button type="button" className="search-screen__row-main" onClick={onClick}>
        <span>{product.tradeName}</span>
        {product.manufacturer !== null && (
          <span className="search-screen__manufacturer">{product.manufacturer}</span>
        )}
      </button>
      <button
        type="button"
        className="search-screen__favorite"
        onClick={onFavoriteToggle}
        aria-label={isFavorite ? "Прибрати з обраних" : "Додати в обрані"}
      >
        {isFavorite ? "★" : "☆"}
      </button>
    </div>
  )
}
