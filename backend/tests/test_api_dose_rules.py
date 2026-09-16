from app.models import DoseRule


def test_verify_dose_rule(client, admin_headers, seeded_db):
    rule = seeded_db.query(DoseRule).first()
    rule.is_verified = False
    seeded_db.commit()
    assert rule.is_verified is False

    response = client.post(f"/api/v1/dose-rules/{rule.id}/verify", headers=admin_headers)
    assert response.status_code == 200
    assert response.json()["is_verified"] is True


def test_verify_requires_admin_key(client, seeded_db):
    rule = seeded_db.query(DoseRule).first()
    response = client.post(f"/api/v1/dose-rules/{rule.id}/verify")
    assert response.status_code == 401


def test_create_dose_rule_rejects_inverted_range(client, admin_headers, seeded_db):
    from app.models import Species, Substance

    substance = seeded_db.query(Substance).first()
    species = seeded_db.query(Species).first()
    response = client.post(
        "/api/v1/dose-rules",
        json={
            "substance_id": str(substance.id),
            "species_id": str(species.id),
            "route": "po",
            "dose_min": "20",
            "dose_max": "10",
            "dose_unit": "mg_per_kg",
            "source": "Plumb's Veterinary Drug Handbook (9th Ed.)",
        },
        headers=admin_headers,
    )
    assert response.status_code == 422
    assert response.json()["error"]["code"] == "VALIDATION_ERROR"
