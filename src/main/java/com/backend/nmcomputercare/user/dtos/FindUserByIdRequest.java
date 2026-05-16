package com.backend.nmcomputercare.user.dtos;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder; import lombok.Data;
@Data @Builder
public class FindUserByIdRequest implements RequestContract { private Long id; }
