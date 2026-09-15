from app.models import DoseRule, Product, Species, Substance


def _amoxicillin_dog_setup(seeded_db):
    substance = seeded_db.query(Substance).filter_by(name="TEST_Substance_Amoxicillin_Analog").one()
    species = seeded_db.query(Species).filter_by(code="dog").one()
    dose_rule = (
        seeded_db.query(DoseRule).filter_by(substance_id=substance.id, species_id=species.id).one()
    )
    product = seeded_db.query(Product).filter_by(trade_name="TEST_Antibiotic_Inj_A").one()
    return species, dose_rule, product


def test_calculate_success(client, seeded_db):
    species, dose_rule, product = _amoxicillin_dog_setup(seeded_db)
    response = client.post(
        "/api/v1/calculate",
        json={
            "weight_kg": "10",
            "species_id": str(species.id),
            "dose_rule_id": str(dose_rule.id),
            "product_id": str(product.id),
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["administration_unit"] == "ml"
    assert isinstance(body["administration_min"], str)
    assert "RULE_NOT_VERIFIED" in [w["code"] for w in body["warnings"]]
    assert len(body["explanation"]) >= 1


def test_calculate_species_mismatch(client, seeded_db):
    _, dose_rule, product = _amoxicillin_dog_setup(seeded_db)
    cat = seeded_db.query(Species).filter_by(code="cat").one()
    response = client.post(
        "/api/v1/calculate",
        json={
            "weight_kg": "10",
            "species_id": str(cat.id),
            "dose_rule_id": str(dose_rule.id),
            "product_id": str(product.id),
        },
    )
    assert response.status_code == 400
    assert response.json()["error"]["code"] == "SPECIES_MISMATCH"


def test_calculate_substance_mismatch(client, seeded_db):
    species, dose_rule, _ = _amoxicillin_dog_setup(seeded_db)
    other_product = seeded_db.query(Product).filter_by(trade_name="TEST_Insulin_Inj_A").one()
    response = client.post(
        "/api/v1/calculate",
        json={
            "weight_kg": "10",
            "species_id": str(species.id),
            "dose_rule_id": str(dose_rule.id),
            "product_id": str(other_product.id),
        },
    )
    assert response.status_code == 400
    assert response.json()["error"]["code"] == "SUBSTANCE_MISMATCH"


def test_calculate_invalid_weight_maps_to_calculator_error_code(client, seeded_db):
    species, dose_rule, product = _amoxicillin_dog_setup(seeded_db)
    response = client.post(
        "/api/v1/calculate",
        json={
            "weight_kg": "0",
            "species_id": str(species.id),
            "dose_rule_id": str(dose_rule.id),
            "product_id": str(product.id),
        },
    )
    assert response.status_code == 422
    assert response.json()["error"]["code"] == "INVALID_WEIGHT"


def test_calculate_not_found(client, seeded_db):
    species, dose_rule, product = _amoxicillin_dog_setup(seeded_db)
    response = client.post(
        "/api/v1/calculate",
        json={
            "weight_kg": "10",
            "species_id": "00000000-0000-0000-0000-000000000000",
            "dose_rule_id": str(dose_rule.id),
            "product_id": str(product.id),
        },
    )
    assert response.status_code == 404
    assert response.json()["error"]["code"] == "NOT_FOUND"
