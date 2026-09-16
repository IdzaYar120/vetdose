from app.models import Species


def test_search_products_by_trade_name(client, seeded_db):
    response = client.get("/api/v1/products", params={"search": "Amoxoil Retard 150 mg/ml"})
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 1
    assert body[0]["trade_name"] == "Amoxoil Retard 150 mg/ml"
    assert body[0]["substance_name_uk"]


def test_search_products_by_substance_name(client, seeded_db):
    response = client.get("/api/v1/products", params={"search": "Амоксицилін"})
    assert response.status_code == 200
    body = response.json()
    assert len(body) >= 3  # amox_inj, amox_tab_250, amox_tab_50


def test_search_products_filtered_by_species(client, seeded_db):
    cattle_id = str(seeded_db.query(Species).filter_by(code="cattle").one().id)
    response = client.get("/api/v1/products", params={"species_id": cattle_id})
    assert response.status_code == 200
    trade_names = {p["trade_name"] for p in response.json()}
    assert "Amoxoil Retard 150 mg/ml" in trade_names  # has a cattle dose_rule
    assert "Vetmedin 2.5 mg" not in trade_names  # pimobendan is only dosed for dog


def test_product_detail_includes_rules_contraindications_withdrawal(client, seeded_db):
    list_resp = client.get(
        "/api/v1/products", params={"search": "Amoxoil Retard 150 mg/ml"}
    ).json()
    product_id = list_resp[0]["id"]

    detail_resp = client.get(f"/api/v1/products/{product_id}")
    assert detail_resp.status_code == 200
    detail = detail_resp.json()
    assert detail["trade_name"] == "Amoxoil Retard 150 mg/ml"
    assert len(detail["dose_rules"]) >= 1
    assert any(c["source"] for c in detail["contraindications"])
    assert len(detail["withdrawal_periods"]) >= 2
