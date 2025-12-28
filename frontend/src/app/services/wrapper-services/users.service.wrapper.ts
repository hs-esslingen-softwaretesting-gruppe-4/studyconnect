import { Injectable } from "@angular/core";
import { UsersService } from "../../api-services/api/api/users.service";
import { UserCreateRequest, UserResponse } from "../../api-services/api";
import { lastValueFrom } from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class UsersServiceWrapper {
  constructor(private readonly usersService: UsersService) {}

  async createUser(user: UserCreateRequest): Promise<UserResponse> {
    return await lastValueFrom(this.usersService.apiUsersPost(user));
  }

}
