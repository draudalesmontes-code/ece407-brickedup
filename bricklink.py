import requests
from requests_oauthlib import OAuth1
import json
from typing import Optional, Dict, Any
from datetime import datetime


class BricklinkAPI:

    
    BASE_URL = "https://api.bricklink.com/api/store/v1"
    
    def __init__(self, consumer_key: str, consumer_secret: str, 
                 token_value: str, token_secret: str):

        self.consumer_key = consumer_key
        self.consumer_secret = consumer_secret
        self.token_value = token_value
        self.token_secret = token_secret
        
        #Seting up OAuth1 authentication
        self.auth = OAuth1(
            client_key=self.consumer_key,
            client_secret=self.consumer_secret,
            resource_owner_key=self.token_value,
            resource_owner_secret=self.token_secret,
            signature_method='HMAC-SHA1',
            signature_type='AUTH_HEADER'
        )
    
    #makes the request :D
    def _make_request(self, method: str, endpoint: str, 
                     params: Optional[Dict] = None, 
                     data: Optional[Dict] = None) -> Dict[str, Any]:

        url = f"{self.BASE_URL}/{endpoint.lstrip('/')}"
        
        headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
        
        try:
            response = requests.request(
                method=method,
                url=url,
                auth=self.auth,
                params=params,
                json=data,
                headers=headers
            )
            
            response.raise_for_status()
            
            return response.json()
            
        except requests.exceptions.RequestException as e:
            print(f"Request failed: {e}")
            if hasattr(e.response, 'text'):
                print(f"Response: {e.response.text}")
            raise
    
    #getting the request
    def get(self, endpoint: str, params: Optional[Dict] = None) -> Dict[str, Any]:
        return self._make_request('GET', endpoint, params=params)
    
    #posting the request
    def post(self, endpoint: str, data: Dict, params: Optional[Dict] = None) -> Dict[str, Any]:
        return self._make_request('POST', endpoint, params=params, data=data)
    
    def put(self, endpoint: str, data: Dict, params: Optional[Dict] = None) -> Dict[str, Any]:
        return self._make_request('PUT', endpoint, params=params, data=data)
    
    def delete(self, endpoint: str, params: Optional[Dict] = None) -> Dict[str, Any]:
        return self._make_request('DELETE', endpoint, params=params)
    
    #getting the order informations
    
    def get_orders(self, direction: str = "in", status: Optional[str] = None) -> Dict[str, Any]:
        params = {"direction": direction}
        if status:
            params["status"] = status
        return self.get("orders", params=params)
    
    def get_order(self, order_id: int) -> Dict[str, Any]:
        return self.get(f"orders/{order_id}")
    
    def get_inventory(self) -> Dict[str, Any]:
        return self.get("inventories")
    
    def get_item(self, item_type: str, item_no: str) -> Dict[str, Any]:

        return self.get(f"items/{item_type}/{item_no}")
    
    
    #getting the price guide on bricklink
    def get_price_guide(self, item_type: str, item_no: str, 
                       color_id: Optional[int] = None,
                       guide_type: str = "stock",
                       new_or_used: str = "N",
                       country_code: Optional[str] = None,
                       region: Optional[str] = None,
                       currency_code: Optional[str] = None,
                       vat: str = "N") -> Dict[str, Any]:
       
        params = {
            "guide_type": guide_type,
            "new_or_used": new_or_used,
            "vat": vat
        }
        
        if color_id is not None:
            params["color_id"] = color_id
        if country_code:
            params["country_code"] = country_code
        if region:
            params["region"] = region
        if currency_code:
            params["currency_code"] = currency_code
        
        return self.get(f"items/{item_type}/{item_no}/price", params=params)
    
    #getting the price guide for sets
    def get_set_price_guide(self, set_no: str, **kwargs) -> Dict[str, Any]:

        return self.get_price_guide("SET", set_no, **kwargs)


#testing
if __name__ == "__main__":
    CONSUMER_KEY = ""
    CONSUMER_SECRET = ""
    TOKEN_VALUE = ""
    TOKEN_SECRET = ""
    
    #setting up the client
    client = BricklinkAPI(
        consumer_key=CONSUMER_KEY,
        consumer_secret=CONSUMER_SECRET,
        token_value=TOKEN_VALUE,
        token_secret=TOKEN_SECRET
    )
    
    try:
        #example
        print("Fetching price guide for...")
        print("New")
        
        price_guide = client.get_set_price_guide(
            "10497-1",
            guide_type="sold",
            country_code="US",
            currency_code="USD"
        )
        
        print(json.dumps(price_guide, indent=2))
        
    except Exception as e:
        print(f"Error: {e}")