import { Injectable } from "@angular/core";
import { UsersService } from "../../api-services/api/api/users.service";
import { UserCreateRequest, UserResponse } from "../../api-services/api";
import { lastValueFrom } from "rxjs";
import { AutocompleteType } from "../../models/autocomplete-type";

@Injectable({
  providedIn: 'root',
})
export class UsersServiceWrapper {
  constructor(private readonly usersService: UsersService) {}

  async createUser(user: UserCreateRequest): Promise<UserResponse> {
    return await lastValueFrom(this.usersService.apiUsersPost(user));
  }

  async getUserIDByToken(): Promise<number | undefined> {
    try {
    const userResponse =  await lastValueFrom(this.usersService.apiUsersMeGet());
    return userResponse.id;
    } catch (error) {
      console.error('Failed to get user ID by token:', error);
      return undefined;
    }
  }

  async getAllUsers(): Promise<UserResponse[]> {
    return await lastValueFrom(this.usersService.apiUsersGet());
  }

  async getAllUsersFormattedForAutocomplete(): Promise<AutocompleteType[]> {
    const users = await this.getAllUsers();
    return users.map(user => ({
      label: `${user.firstname} ${user.lastname} (${user.email})`,
      value: user.id
    }));
  }

}
