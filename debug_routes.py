from app.main import app
import inspect

for r in app.routes:
    if getattr(r, "path", None) == "/products/recommend":
        ep = r.endpoint
        print("PATH =", r.path)
        print("NAME =", getattr(ep, "__name__", None))
        print("MODULE =", getattr(ep, "__module__", None))
        try:
            print("FILE =", inspect.getsourcefile(ep))
            print("LINE =", inspect.getsourcelines(ep)[1])
        except Exception as e:
            print("SRC_ERR =", e)
