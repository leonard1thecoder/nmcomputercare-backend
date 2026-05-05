package com.backend.nmcomputercare.utils;

import java.util.List;

public interface ExecuteService {
    List<? extends ResponseContract> callable(String serviceName, RequestContract requestContract);
}
