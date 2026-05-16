package com.backend.nmcomputercare.businesscare.dtos;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder; import lombok.Data;
@Data @Builder
public class FindBusinessCarePlanByIdRequest implements RequestContract { private Long id; }
