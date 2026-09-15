from datetime import UTC, datetime, timedelta


def test_full_sync_returns_all_seeded_rows(client, seeded_db):
    response = client.get("/api/v1/sync")
    assert response.status_code == 200
    body = response.json()
    assert "server_time" in body
    assert len(body["species"]) == 9
    assert len(body["substances"]) >= 5
    assert len(body["products"]) >= 8
    assert len(body["dose_rules"]) >= 5
    assert len(body["contraindications"]) >= 1
    assert len(body["withdrawal_periods"]) >= 2


def test_incremental_sync_since_future_returns_nothing(client, seeded_db):
    future = (datetime.now(UTC) + timedelta(days=1)).isoformat()
    response = client.get("/api/v1/sync", params={"since": future})
    assert response.status_code == 200
    body = response.json()
    assert body["species"] == []
    assert body["substances"] == []


def test_sync_includes_soft_deleted_rows(client, admin_headers, seeded_db):
    create_resp = client.post(
        "/api/v1/species",
        json={
            "code": "TEST_sync_species",
            "name_uk": "TEST Вид Sync",
            "is_food_producing": False,
            "typical_min_weight_kg": "1",
            "typical_max_weight_kg": "5",
        },
        headers=admin_headers,
    )
    species_id = create_resp.json()["id"]

    baseline = client.get("/api/v1/sync").json()["server_time"]

    client.delete(f"/api/v1/species/{species_id}", headers=admin_headers)

    response = client.get("/api/v1/sync", params={"since": baseline})
    body = response.json()
    deleted_rows = [s for s in body["species"] if s["id"] == species_id]
    assert len(deleted_rows) == 1
    assert deleted_rows[0]["is_deleted"] is True
