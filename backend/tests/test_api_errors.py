import uuid


def test_404_uses_uniform_error_shape(client):
    response = client.get(f"/api/v1/species/{uuid.uuid4()}", headers={"X-API-Key": "wrong"})
    assert response.status_code == 401
    body = response.json()
    assert body["error"]["code"] == "UNAUTHORIZED"


def test_admin_route_without_key_is_unauthorized(client, seeded_db):
    response = client.post("/api/v1/species", json={})
    assert response.status_code == 401
    body = response.json()
    assert "error" in body
    assert body["error"]["code"] == "UNAUTHORIZED"


def test_not_found_uses_uniform_error_shape(client, admin_headers, seeded_db):
    response = client.get(f"/api/v1/species/{uuid.uuid4()}", headers=admin_headers)
    assert response.status_code == 404
    body = response.json()
    assert body["error"]["code"] == "NOT_FOUND"


def test_validation_error_uses_uniform_error_shape(client, admin_headers):
    response = client.post(
        "/api/v1/species",
        json={"code": "cat", "name_uk": "Кіт", "is_food_producing": False},
        headers=admin_headers,
    )
    assert response.status_code == 422
    body = response.json()
    assert body["error"]["code"] == "VALIDATION_ERROR"
    assert "message" in body["error"]
