# Copyright (C) 2011-2012  The OpenTSDB Authors.
#
# This library is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published
# by the Free Software Foundation, either version 2.1 of the License, or
# (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this library.  If not, see <http://www.gnu.org/licenses/>.

THRIFT_VERSION := 0.3.0
THRIFT := third_party/thrift/thrift-$(THRIFT_VERSION).jar
THRIFT_BASE_URL := http://clojars.org/repo/org/clojars/gfodor/thrift/$(THRIFT_VERSION)

$(THRIFT): $(THRIFT).md5
	set dummy "$(THRIFT_BASE_URL)" "$(THRIFT)"; shift; $(FETCH_DEPENDENCY)

THIRD_PARTY += $(THRIFT)
