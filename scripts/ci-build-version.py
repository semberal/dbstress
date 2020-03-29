#!/usr/bin/env python3
import os

if "GITHUB_REF" in os.environ:
    ref: str = os.environ["GITHUB_REF"]
    if ref.startswith("refs/tags/"):
        print(ref.split("/")[-1])
    elif ref.startswith("refs/heads/"):
        print(f"0.0.{os.environ['GITHUB_RUN_NUMBER']}-SNAPSHOT")
else:
    raise Exception("Not GitHub CI build")
