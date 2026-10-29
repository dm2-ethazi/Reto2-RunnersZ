"""Pruebas de contrato HTTP de la API."""
import os
from fastapi.testclient import TestClient
from runnersz.main import app
client=TestClient(app)
def headers(): return {"Authorization":"Bearer test-token"}
def test_health_without_token():
    response=client.get('/api/v1/health'); assert response.status_code==200; assert response.json()['status']=='ok'
def test_auth_and_text(monkeypatch):
    monkeypatch.setenv('RZ_API_TOKEN','test-token')
    assert client.get('/api/v1/auth/check',headers=headers()).status_code==204
    assert client.post('/api/v1/text/caesar',json={'text':'Ataque','shift':3},headers=headers()).json()=={'result':'Dwdtxh'}
def test_rejects_missing_token(monkeypatch):
    monkeypatch.setenv('RZ_API_TOKEN','test-token')
    response=client.post('/api/v1/text/caesar',json={'text':'A','shift':3}); assert response.status_code==401; assert response.json()['code']=='UNAUTHORIZED'
def test_file_roundtrip(monkeypatch):
    monkeypatch.setenv('RZ_API_TOKEN','test-token'); source=b'contenido RunnersZ'
    encrypted=client.post('/api/v1/files/encrypt',files={'file':('a.txt',source)},data={'password':'clave'},headers=headers())
    assert encrypted.status_code==200
    decrypted=client.post('/api/v1/files/decrypt',files={'file':('a.enc',encrypted.content)},data={'password':'clave'},headers=headers())
    assert decrypted.status_code==200 and decrypted.content==source and decrypted.headers['x-integrity']=='verified'
def test_empty_file(monkeypatch):
    monkeypatch.setenv('RZ_API_TOKEN','test-token')
    response=client.post('/api/v1/files/encrypt',files={'file':('a.txt',b'')},data={'password':'clave'},headers=headers())
    assert response.status_code==400 and response.json()['code']=='EMPTY_FILE'
