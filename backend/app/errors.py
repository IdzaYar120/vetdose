from fastapi import Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


class APIError(Exception):
    """Raised anywhere in the API layer for an error the client should see
    in VetDose's uniform ``{"error": {"code", "message"}}`` shape.
    """

    def __init__(
        self, code: str, message: str, status_code: int = status.HTTP_400_BAD_REQUEST
    ) -> None:
        self.code = code
        self.message = message
        self.status_code = status_code
        super().__init__(message)


def _error_response(code: str, message: str, status_code: int) -> JSONResponse:
    return JSONResponse(
        status_code=status_code, content={"error": {"code": code, "message": message}}
    )


async def api_error_handler(request: Request, exc: Exception) -> JSONResponse:
    assert isinstance(exc, APIError)
    return _error_response(exc.code, exc.message, exc.status_code)


async def http_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    assert isinstance(exc, StarletteHTTPException)
    code = "NOT_FOUND" if exc.status_code == status.HTTP_404_NOT_FOUND else "HTTP_ERROR"
    detail = exc.detail if isinstance(exc.detail, str) else str(exc.detail)
    return _error_response(code, detail, exc.status_code)


async def validation_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    assert isinstance(exc, RequestValidationError)
    details = "; ".join(
        f"{'.'.join(str(loc) for loc in error['loc'])}: {error['msg']}" for error in exc.errors()
    )
    return _error_response("VALIDATION_ERROR", details, status.HTTP_422_UNPROCESSABLE_CONTENT)
