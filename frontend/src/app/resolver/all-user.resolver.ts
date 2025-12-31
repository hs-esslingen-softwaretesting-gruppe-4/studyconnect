import { Resolve } from "@angular/router";
import { AutocompleteType } from "../models/autocomplete-type";
import { UsersServiceWrapper } from "../services/wrapper-services/users.service.wrapper";
import { Injectable } from "@angular/core";

@Injectable({
  providedIn: 'root',
})
/**
 * Resolver to load all users formatted for autocomplete before loading a route.
 */
export class AllUserResolver implements Resolve<AutocompleteType[]> {
  constructor(private readonly usersService: UsersServiceWrapper) {}

  async resolve(): Promise<AutocompleteType[]> {
    return await this.usersService.getAllUsersFormattedForAutocomplete();
  }
}
