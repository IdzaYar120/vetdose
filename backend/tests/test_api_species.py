def test_public_list_species(client, seeded_db):
    response = client.get("/api/v1/species")
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 9
    codes = {s["code"] for s in body}
    assert "cat" in codes
    assert "typical_min_weight_kg" in body[0]
    # Decimal fields must round-trip as JSON strings, never numbers.
    assert isinstance(body[0]["typical_min_weight_kg"], str)


def test_admin_create_and_read_species(client, admin_headers):
    payload = {
        "code": "TEST_species",
        "name_uk": "TEST Вид",
        "is_food_producing": False,
        "typical_min_weight_kg": "1",
        "typical_max_weight_kg": "5",
    }
    create_resp = client.post("/api/v1/species", json=payload, headers=admin_headers)
    assert create_resp.status_code == 201
    created = create_resp.json()
    assert created["code"] == "TEST_species"

    get_resp = client.get(f"/api/v1/species/{created['id']}", headers=admin_headers)
    assert get_resp.status_code == 200
    assert get_resp.json()["name_uk"] == "TEST Вид"


def test_admin_update_species(client, admin_headers):
    payload = {
        "code": "TEST_species2",
        "name_uk": "TEST Вид 2",
        "is_food_producing": False,
        "typical_min_weight_kg": "1",
        "typical_max_weight_kg": "5",
    }
    created = client.post("/api/v1/species", json=payload, headers=admin_headers).json()

    update_resp = client.patch(
        f"/api/v1/species/{created['id']}",
        json={"name_uk": "TEST Вид 2 (оновлено)"},
        headers=admin_headers,
    )
    assert update_resp.status_code == 200
    updated = update_resp.json()
    assert updated["name_uk"] == "TEST Вид 2 (оновлено)"
    assert updated["code"] == "TEST_species2"


def test_admin_delete_species_is_soft_and_hidden_from_public_list(client, admin_headers):
    payload = {
        "code": "TEST_species3",
        "name_uk": "TEST Вид 3",
        "is_food_producing": False,
        "typical_min_weight_kg": "1",
        "typical_max_weight_kg": "5",
    }
    created = client.post("/api/v1/species", json=payload, headers=admin_headers).json()

    delete_resp = client.delete(f"/api/v1/species/{created['id']}", headers=admin_headers)
    assert delete_resp.status_code == 204

    list_resp = client.get("/api/v1/species")
    assert created["id"] not in {s["id"] for s in list_resp.json()}

    get_resp = client.get(f"/api/v1/species/{created['id']}", headers=admin_headers)
    assert get_resp.status_code == 404
