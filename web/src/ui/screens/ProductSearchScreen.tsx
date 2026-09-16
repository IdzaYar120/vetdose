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
        <div className="field">
          <input
            type="search"
            placeholder="🔍 Назва препарату або діюча речовина..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoFocus
          />
        </div>

        {species != null && (
          <div>
            <button
              type="button"
              className={`chip${speciesFilterEnabled ? " chip--selected" : ""}`}
              onClick={() => setSpeciesFilterEnabled((v) => !v)}
            >
              {speciesFilterEnabled ? `Вид: ${species.nameUk}` : "Усі види"}
            </button>
          </div>
        )}

        {isEmpty ? (
          <div className="empty-state">
            <span>🔎</span>
            <span>Нічого не знайдено</span>
          </div>
        ) : (
          <div className="search-screen__list">
            {favorites.length > 0 && (
              <>
                <p className="search-screen__section">★ Обрані препарати</p>
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
    <div className="search-screen__card">
      <button type="button" className="search-screen__row-main" onClick={onClick}>
        <span className="search-screen__trade-name">{product.tradeName}</span>
        {product.manufacturer !== null && (
          <span className="search-screen__manufacturer">{product.manufacturer}</span>
        )}
      </button>
      <button
        type="button"
        className={`search-screen__favorite${isFavorite ? " search-screen__favorite--active" : ""}`}
        onClick={onFavoriteToggle}
        aria-label={isFavorite ? "Прибрати з обраних" : "Додати в обрані"}
      >
        {isFavorite ? "★" : "☆"}
      </button>
    </div>
  )
}
