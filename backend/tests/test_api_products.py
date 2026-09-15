from app.models import Species


def test_search_products_by_trade_name(client, seeded_db):
    response = client.get("/api/v1/products", params={"search": "TEST_Antibiotic_Inj_A"})
    assert response.status_code == 200
    body = response.json()
    assert len(body) == 1
    assert body[0]["trade_name"] == "TEST_Antibiotic_Inj_A"
    assert body[0]["substance_name_uk"]


def test_search_products_by_substance_name(client, seeded_db):
    response = client.get("/api/v1/products", params={"search": "Амоксицилін"})
    assert response.status_code == 200
    body = response.json()
    assert len(body) >= 3  # amox_inj, amox_tab, amox_susp


def test_search_products_filtered_by_species(client, seeded_db):
    cattle_id = str(seeded_db.query(Species).filter_by(code="cattle").one().id)
    response = client.get("/api/v1/products", params={"species_id": cattle_id})
    assert response.status_code == 200
    trade_names = {p["trade_name"] for p in response.json()}
    assert "TEST_Antibiotic_Inj_A" in trade_names  # has a cattle dose_rule
    assert "TEST_Insulin_Inj_A" not in trade_names  # only dosed for cat/dog


def test_product_detail_includes_rules_contraindications_withdrawal(client, seeded_db):
    list_resp = client.get("/api/v1/products", params={"search": "TEST_Antibiotic_Inj_A"}).json()
    product_id = list_resp[0]["id"]

    detail_resp = client.get(f"/api/v1/products/{product_id}")
    assert detail_resp.status_code == 200
    detail = detail_resp.json()
    assert detail["trade_name"] == "TEST_Antibiotic_Inj_A"
    assert len(detail["dose_rules"]) >= 1
    assert any(c["source"] for c in detail["contraindications"])
    assert len(detail["withdrawal_periods"]) >= 2
